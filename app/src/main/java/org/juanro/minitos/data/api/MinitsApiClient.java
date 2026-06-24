package org.juanro.minitos.data.api;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.juanro.minitos.data.api.config.NetworkConstants;

import java.io.File;

public class MinitsApiClient {
    private static final String TAG = "MinitsApiClient";
    private static final String BASE_URL = NetworkConstants.MINITS_BASE_URL;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final Context context;
    private String jwtToken;
    private String userEmail;

    public MinitsApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.client = createAuthenticatedClient(this.context);
    }

    private OkHttpClient createAuthenticatedClient(Context context) {
        try {
            X509TrustManager combinedTrustManager = getCombinedTrustManager(context);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{combinedTrustManager}, null);

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), combinedTrustManager)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating OkHttpClient", e);
            return new OkHttpClient();
        }
    }

    @SuppressWarnings("CustomX509TrustManager")
    private X509TrustManager getCombinedTrustManager(Context context) throws Exception {
        // 1. Get the system's default TrustManager
        TrustManagerFactory systemTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        systemTmf.init((KeyStore) null);
        final X509TrustManager systemTrustManager = (X509TrustManager) systemTmf.getTrustManagers()[0];

        // 2. Load ISRG Root X1 certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = context.getAssets().open("certs/isrg_root_x1.pem");
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
            Log.d(TAG, "Loaded custom CA: " + ((X509Certificate) ca).getSubjectDN());
        } finally {
            caInput.close();
        }

        // 3. Create a KeyStore for the custom CA
        KeyStore customKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        customKeyStore.load(null, null);
        customKeyStore.setCertificateEntry("ca", ca);

        // 4. Create a TrustManager for the custom CA
        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(customKeyStore);
        final X509TrustManager customTrustManager = (X509TrustManager) customTmf.getTrustManagers()[0];

        // 5. Create a combined TrustManager
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                systemTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                try {
                    systemTrustManager.checkServerTrusted(chain, authType);
                } catch (java.security.cert.CertificateException e) {
                    // If system fails, try with custom CA
                    customTrustManager.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return systemTrustManager.getAcceptedIssuers();
            }
        };
    }

    public void setAuth(String token, String email) {
        this.jwtToken = token;
        this.userEmail = email;
    }

    public interface Callback<T> {
        void onSuccess(T response);
        void onError(Exception e);
    }

    private String getAppVersion() {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("pref_minits_app_version", "4.9.4.6rel");
    }

    public <T> T post(String path, Object body, Class<T> responseClass) throws Exception {
        String url = BASE_URL + path;
        String jsonBody = gson.toJson(body);
        Log.d(TAG, "Request URL: " + url);
        Log.d(TAG, "Request Body: " + jsonBody);
        
        String appVersion = getAppVersion();
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("User-Agent", "minits/" + appVersion + " (Android)")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Language", "es")
                .addHeader("x-app-version", appVersion)
                .addHeader("Authorization", jwtToken != null && !jwtToken.isEmpty() ? "Bearer " + jwtToken : "Bearer ")
                .addHeader("x-request-sid", userEmail != null ? userEmail : "");

        Request request = builder.build();
        Log.d(TAG, "Full Headers sent: " + request.headers().toString().replace("\n", " | "));
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Log.d(TAG, "Response Code: " + response.code());
            Log.d(TAG, "Response Body: " + responseBody);

            if (response.code() == 401) {
                throw new MinitsAuthException("Session expired (401)");
            }

            if (!response.isSuccessful()) {
                throw new Exception("Unexpected code " + response + " | Body: " + responseBody);
            }
            return gson.fromJson(responseBody, responseClass);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public <T> T postMultipart(String path, Map<String, String> params, File file, String fieldName, Class<T> responseClass) throws Exception {
        String url = BASE_URL + path;
        Log.d(TAG, "Request URL (Multipart): " + url);

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // Add file
        if (file != null && file.exists()) {
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
            multipartBuilder.addFormDataPart(fieldName, file.getName(), fileBody);
        }

        // Add other params
        for (Map.Entry<String, String> entry : params.entrySet()) {
            multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
        }

        RequestBody requestBody = multipartBuilder.build();
        String appVersion = getAppVersion();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("User-Agent", "XOne/" + appVersion + " (Android)")
                .addHeader("Accept", "*/*")
                .addHeader("Content-Language", "es")
                .addHeader("Authorization", jwtToken != null && !jwtToken.isEmpty() ? "Bearer " + jwtToken : "Bearer ")
                .addHeader("x-request-sid", userEmail != null ? userEmail : "")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Log.d(TAG, "Response Code: " + response.code());
            Log.d(TAG, "Response Body: " + responseBody);

            if (response.code() == 401) {
                throw new MinitsAuthException("Session expired (401)");
            }

            if (!response.isSuccessful()) {
                throw new Exception("Unexpected code " + response + " | Body: " + responseBody);
            }
            return gson.fromJson(responseBody, responseClass);
        }
    }
}
