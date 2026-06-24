package org.juanro.minitos.data.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.juanro.minitos.model.MinitosDatabase;
import org.juanro.minitos.model.entity.Vehicle;
import org.juanro.minitos.model.entity.Zone;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;
import org.juanro.minitos.data.api.MinitsParser;
import org.juanro.minitos.data.api.config.NetworkConstants;

import com.google.gson.Gson;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    public static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Default coordinates if not provided in inputData
        double lat = getInputData().getDouble("lat", 38.883);
        double lon = getInputData().getDouble("lon", -6.997);

        try {
            MinitsApiClient minitsClient = new MinitsApiClient(context);
            MinitsAuthenticator authenticator = new MinitsAuthenticator(context, minitsClient);
            authenticator.loadSavedAuth();

            String token = authenticator.getToken();
            String email = authenticator.getEmail();

            if (token == null || token.isEmpty()) {
                try {
                    authenticator.loginAnonymous();
                    token = authenticator.getToken();
                    email = authenticator.getEmail();
                } catch (Exception e) {
                    token = "6a1ff024-74e7-407e-d4aa-75433d73a40d";
                    email = "admin";
                }
            }

            if (token != null && !token.isEmpty()) {
                minitsClient.setAuth(token, email);
            }

            // Fetch Sedes
            List<MinitsModels.Sede> allSedes = new ArrayList<>();
            try {
                MinitsModels.DatosSQLRequest sedesReq = new MinitsModels.DatosSQLRequest("Sedes", Collections.singletonList(""));
                MinitsModels.SedesResponse sedesResp = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, sedesReq, MinitsModels.SedesResponse.class);
                if (sedesResp != null && sedesResp.data != null) {
                    Collections.addAll(allSedes, sedesResp.data);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching sedes", e);
            }

            int currentSedeId = authenticator.getSedeId();
            MinitsModels.Sede currentSede = null;
            for (MinitsModels.Sede s : allSedes) {
                if (s.ID == currentSedeId) {
                    currentSede = s;
                    break;
                }
            }
            if (currentSede == null && !allSedes.isEmpty()) {
                currentSede = allSedes.get(0);
            }

            if (currentSede != null) {
                Gson gson = new Gson();
                // Fetch Zones
                List<Zone> roomZones = new ArrayList<>();
                try {
                    MinitsModels.ZonesResponse zonesResponse = minitsClient.post(NetworkConstants.PATH_ZONAS, Collections.emptyMap(), MinitsModels.ZonesResponse.class);
                    if (zonesResponse != null && zonesResponse.data != null) {
                        for (MinitsModels.Zone z : zonesResponse.data) {
                            roomZones.add(new Zone(z.id, z.fillcolor, z.color, z.width, gson.toJson(z.data)));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching zones", e);
                }

                // Fetch Vehicles
                List<Vehicle> roomVehicles = new ArrayList<>();
                String identifier = (email == null || email.isEmpty() || email.equals("admin")) ? "" : email;
                String officialFilter = "(((w.DEPOSITO*m.KMBATERIA)/100) >=10 AND ((w.DEPOSITO*m.KMBATERIA)/100) <=110 AND m.MODELO IN ('x','Carver','Twizy','Patinete','Bici'))";
                String center = lat + "," + lon;

                MinitsModels.DatosSQLRequest mapReq = new MinitsModels.DatosSQLRequest("DatosMapaMT", List.of(officialFilter, center, identifier));
                MinitsModels.VehiclesResponse vehicleResponse = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, mapReq, MinitsModels.VehiclesResponse.class);

                if (vehicleResponse != null && vehicleResponse.data != null) {
                    MinitsParser minitsParser = new MinitsParser();
                    roomVehicles.addAll(minitsParser.parseVehicles(List.of(vehicleResponse.data)));
                }

                // Fetch Parkings
                try {
                    MinitsModels.DatosSQLRequest reqPark = new MinitsModels.DatosSQLRequest("Parkings", List.of(String.valueOf(currentSede.ID)));
                    MinitsModels.ParkingsResponse parkingsResponse = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, reqPark, MinitsModels.ParkingsResponse.class);
                    if (parkingsResponse != null && parkingsResponse.data != null) {
                        for (MinitsModels.Parking p : parkingsResponse.data) {
                            double pLat, pLon;
                            try { pLat = Double.parseDouble(p.LAT); pLon = Double.parseDouble(p.LON); } catch (Exception e) { continue; }
                            Vehicle s = new Vehicle("park_"+p.ID, p.NOMBRE, "", pLat, pLon, "minits_parking");
                            s.setAddress(p.DIRECCION);
                            roomVehicles.add(s);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching parkings", e);
                }

                // Update Database
                MinitosDatabase db = MinitosDatabase.getInstance(context);
                db.getVehicleDao().updateVehicles(roomVehicles);
                db.getZoneDao().updateZones(roomZones);

                sharedPref.edit()
                        .putLong(PREF_KEY_DB_LAST_UPDATE, System.currentTimeMillis())
                        .apply();
                
                return Result.success();
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            return Result.retry();
        }

        return Result.failure();
    }
}
