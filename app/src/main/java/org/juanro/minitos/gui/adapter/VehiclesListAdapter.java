package org.juanro.minitos.gui.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Locale;

import org.juanro.minitos.R;
import org.juanro.minitos.model.entity.Vehicle;
import org.juanro.minitos.model.entity.VehicleWithFavorite;

public class VehiclesListAdapter extends ArrayAdapter<VehicleWithFavorite> {
    private final Context context;

    public VehiclesListAdapter(Context context, ArrayList<VehicleWithFavorite> vehicles) {
        super(context, R.layout.vehicle_list_item, vehicles);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.vehicle_list_item, parent, false);
        }

        VehicleWithFavorite vehicleWithFavorite = getItem(position);

        if (vehicleWithFavorite != null) {
            Vehicle vehicle = vehicleWithFavorite.getVehicle();
            TextView vehicleName = v.findViewById(R.id.vehicleName);
            TextView vehicleAddress = v.findViewById(R.id.vehicleAddress);
            TextView vehicleDistance = v.findViewById(R.id.vehicleDistance);
            TextView tvMatricula = v.findViewById(R.id.tvListMatricula);
            TextView tvStatus = v.findViewById(R.id.tvListStatus);
            TextView tvBattery = v.findViewById(R.id.tvListBattery);

            vehicleName.setText(vehicle.getName());
            if (vehicle.getAddress() != null) {
                vehicleAddress.setText(vehicle.getAddress());
                vehicleAddress.setVisibility(View.VISIBLE);
            } else {
                vehicleAddress.setVisibility(View.GONE);
            }

            // Populate new fields from extraData
            try {
                org.json.JSONObject extra = new org.json.JSONObject(vehicle.getExtraData());
                tvMatricula.setText(extra.optString("matricula", "--"));
                String estado = extra.optString("estado", "DESCONOCIDO");
                tvStatus.setText(estado);
                
                if ("LIBRE".equals(estado)) {
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#00B857")); // Green
                } else {
                    tvStatus.setTextColor(android.graphics.Color.GRAY);
                }
                
                tvBattery.setText(extra.optString("autonomia", "--% / -- Km"));
            } catch (Exception e) {
                tvMatricula.setText("--");
                tvStatus.setText("--");
                tvBattery.setText("--");
            }

            // Calculate distance to user
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location userLocation = null;
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (userLocation == null) {
                        userLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                } catch (SecurityException ignored) {}
            }

            if (userLocation != null) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        vehicle.getLatitude(), vehicle.getLongitude(), results);
                vehicleDistance.setText(String.format(Locale.US, "%.2f Km", results[0] / 1000.0));
                vehicleDistance.setVisibility(View.VISIBLE);
            } else {
                vehicleDistance.setVisibility(View.GONE);
            }
        }

        return v;
    }
}
