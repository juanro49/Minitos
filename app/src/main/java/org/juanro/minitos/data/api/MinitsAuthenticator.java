package org.juanro.minitos.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.juanro.minitos.data.api.config.NetworkConstants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class MinitsAuthenticator {
    private static final String TAG = "MinitsAuthenticator";
    private static final String PREFS_NAME = "MinitsPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_ADDRESS = "user_address";
    private static final String KEY_CP = "user_cp";
    private static final String KEY_CITY = "user_city";
    private static final String KEY_BIRTH = "user_birth";
    private static final String KEY_SEDE_ID = "user_sede_id";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_PREFIX = "user_prefix";

    private final Context context;
    private final MinitsApiClient apiClient;

    public MinitsAuthenticator(Context context, MinitsApiClient apiClient) {
        this.context = context;
        this.apiClient = apiClient;
    }

    public void login(String username, String password) throws Exception {
        String hashedPassword = sha256(password);
        Map<String, String> body = new HashMap<>();
        body.put("Username", username);
        body.put("Password", hashedPassword);

        MinitsModels.AuthResponse[] response = apiClient.post(NetworkConstants.PATH_AUTHENTICATE, body, MinitsModels.AuthResponse[].class);

        if (response != null && response.length > 0) {
            MinitsModels.AuthResponse authData = response[0];
            saveAuth(authData);
            apiClient.setAuth(authData.TOKEN, authData.EMAIL);
        } else {
            throw new Exception("Login failed: invalid response format");
        }
    }

    public void loginAnonymous() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("TELEFONO", "0000");
        body.put("CODIGOSMS", "0000");

        MinitsModels.LoginResponse response = apiClient.post(NetworkConstants.PATH_LOGIN, body, MinitsModels.LoginResponse.class);

        if (response != null && response.data != null && response.data.TOKEN != null) {
            String token = response.data.TOKEN;
            String email = "No Registrado";
            
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_TOKEN, token)
                    .putString(KEY_EMAIL, email)
                    .apply();
                    
            apiClient.setAuth(token, email);
        } else {
            throw new Exception("Anonymous login failed: " + (response != null ? response.message : "null"));
        }
    }

    public void logout() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        apiClient.setAuth(null, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getName() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_NAME, "");
    }

    public String getAddress() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ADDRESS, "");
    }

    public String getCP() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CP, "");
    }

    public String getCity() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CITY, "");
    }

    public String getBirthDate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BIRTH, "");
    }

    public String getPhone() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PHONE, "");
    }

    public String getPrefix() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PREFIX, "+34");
    }

    public int getSedeId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SEDE_ID, 1); // Default to 1 (Badajoz)
    }

    public void saveAuth(MinitsModels.AuthResponse auth) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_TOKEN, auth.TOKEN)
                .putString(KEY_EMAIL, auth.EMAIL)
                .putString(KEY_NAME, auth.NOMBRE)
                .putString(KEY_ADDRESS, auth.DIRECCION)
                .putString(KEY_CP, auth.CP)
                .putString(KEY_CITY, auth.POBLACION)
                .putString(KEY_BIRTH, auth.FECHANAC)
                .putInt(KEY_SEDE_ID, auth.IDSEDE)
                .putString(KEY_PHONE, auth.TELEFONO)
                .putString(KEY_PREFIX, auth.PREFIJO)
                .apply();
    }

    public void updateSedeId(int sedeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_SEDE_ID, sedeId).apply();
    }

    public void loadSavedAuth() {
        String token = getToken();
        String email = getEmail();
        if (token != null) {
            apiClient.setAuth(token, email);
        }
    }

    public void checkSession(MinitsApiClient.Callback<MinitsModels.AccountStatusResponse> callback) {
        new Thread(() -> {
            try {
                MinitsModels.AccountStatusResponse response = apiClient.post(NetworkConstants.PATH_ESTADO_CUENTA, new HashMap<>(), MinitsModels.AccountStatusResponse.class);
                callback.onSuccess(response);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    private String sha256(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                if (h.length() < 2) hexString.append("0");
                hexString.append(h);
            }
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 error", e);
            return "";
        }
    }
}
