package org.juanro.minitos.data.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import org.json.JSONObject;

import org.juanro.minitos.model.entity.Vehicle;

public class MinitsParser {
    public static final String MINITS_NETWORK_ID = "minits";

    public List<Vehicle> parseVehicles(List<MinitsModels.Vehicle> vehicles) {
        List<Vehicle> vehiclesList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String now = sdf.format(new Date());

        for (MinitsModels.Vehicle vehicle : vehicles) {
            double lat;
            double lon;
            try {
                lat = Double.parseDouble(vehicle.LAT);
                lon = Double.parseDouble(vehicle.LON);
            } catch (Exception e) {
                // Skip if coordinates are invalid
                continue;
            }

            Vehicle vehicleEntity = new Vehicle(
                    vehicle.MATRICULA,
                    Objects.requireNonNullElse(vehicle.MAP_VEHICLEDESCRIPTION, vehicle.MATRICULA),
                    now,
                    lat,
                    lon,
                    MINITS_NETWORK_ID
            );
            vehicleEntity.setAddress(vehicle.MAP_DIRECCION);
            vehicleEntity.setExtraData(serializeExtraData(vehicle).toString());

            vehiclesList.add(vehicleEntity);
        }
        return vehiclesList;
    }

    private JSONObject serializeExtraData(MinitsModels.Vehicle vehicle) {
        JSONObject extra = new JSONObject();
        try {
            extra.put("modelo", vehicle.MAP_MODELO);
            extra.put("matricula", vehicle.MATRICULA);
            extra.put("estado", vehicle.ESTADO != null ? vehicle.ESTADO : vehicle.MAP_ESTADO);
            extra.put("color", vehicle.MAP_COLOR);
            extra.put("bateria", vehicle.MAP_BATERIA);
            extra.put("autonomia", vehicle.MAP_VEHICLEBATTERYKMS);
            extra.put("tarifa_activa", vehicle.MAP_IMPORTETARIFA_ACTIVA);
            extra.put("tarifa_standby", vehicle.MAP_IMPORTETARIFA_STANDBY);
            extra.put("pasajeros", vehicle.MAP_VEHICLEPASSENGERS);
            extra.put("motor", vehicle.MAP_VEHICLEENGINE);
            extra.put("transmision", vehicle.MAP_VEHICLETRANSMISSION);
        } catch (Exception e) {
            // Ignore serialization errors
        }
        return extra;
    }
}