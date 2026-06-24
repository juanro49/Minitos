package org.juanro.minitos.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.juanro.minitos.data.api.config.NetworkConstants;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenRouteServiceClient {
    private static final String TAG = "OpenRouteServiceClient";
    private static final String BASE_URL = NetworkConstants.ORS_BASE_URL + NetworkConstants.PATH_ORS_DIRECTIONS;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    private final Context context;

    public OpenRouteServiceClient(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public interface Callback {
        void onSuccess(String geoJson);
        void onError(Exception e);
    }

    private String getApiKey() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("pref_ors_api_key", "");
    }

    public void getWalkingRoute(double startLat, double startLon, double endLat, double endLon, Callback callback) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            callback.onError(new Exception("API Key not provided"));
            return;
        }

        new Thread(() -> {
            try {
                JsonObject body = new JsonObject();
                JsonArray coordinates = new JsonArray();
                
                JsonArray start = new JsonArray();
                start.add(startLon);
                start.add(startLat);
                
                JsonArray end = new JsonArray();
                end.add(endLon);
                end.add(endLat);
                
                coordinates.add(start);
                coordinates.add(end);
                
                body.add("coordinates", coordinates);
                body.addProperty("units", "m");
                body.addProperty("language", "es");

                String jsonBody = gson.toJson(body);
                RequestBody requestBody = RequestBody.create(jsonBody, JSON);

                Request request = new Request.Builder()
                        .url(BASE_URL)
                        .post(requestBody)
                        .addHeader("Authorization", apiKey)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new Exception("Error in ORS: " + response.code() + " " + response.message());
                    }
                    String responseBody = response.body().string();
                    callback.onSuccess(responseBody);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route", e);
                callback.onError(e);
            }
        }).start();
    }
}
