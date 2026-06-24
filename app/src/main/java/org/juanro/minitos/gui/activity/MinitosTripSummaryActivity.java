package org.juanro.minitos.gui.activity;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;
import org.juanro.minitos.data.api.config.NetworkConstants;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.Point;

public class MinitosTripSummaryActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MinitosTripSummary";
    private static final String ROUTE_SOURCE_ID = "route-source";
    private static final String ROUTE_LAYER_ID = "route-layer";

    private MapView mapView;
    private MapLibreMap map;
    private MinitsApiClient minitsClient;
    private List<LatLng> routePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapLibre.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minitos_trip_summary);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + 72);
            return insets;
        });

        mapView = findViewById(R.id.mapSummary);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        String vehicle = getIntent().getStringExtra("vehicle");
        String dateRange = getIntent().getStringExtra("dateRange");
        double totalCost = getIntent().getDoubleExtra("totalCost", 0.0);
        double standbyCost = getIntent().getDoubleExtra("standbyCost", 0.0);
        double drivingCost = getIntent().getDoubleExtra("drivingCost", 0.0);
        String distance = getIntent().getStringExtra("distance");
        
        int idReserva = getIntent().getIntExtra("idReserva", -1);
        int year = getIntent().getIntExtra("year", -1);
        int month = getIntent().getIntExtra("month", -1);

        String startTime = "--";
        String endTime = "--";
        if (dateRange != null && dateRange.contains(" - ")) {
            String[] parts = dateRange.split(" - ");
            if (parts.length == 2) {
                startTime = parts[0].trim();
                endTime = parts[1].trim();
            } else {
                startTime = dateRange;
            }
        }

        ((TextView) findViewById(R.id.tvSummaryVehicle)).setText(vehicle != null ? vehicle : "Minits");
        ((TextView) findViewById(R.id.tvSummaryStart)).setText(startTime);
        ((TextView) findViewById(R.id.tvSummaryEnd)).setText(endTime);
        ((TextView) findViewById(R.id.tvSummaryDistance)).setText(distance != null ? distance : "0 Km");
        ((TextView) findViewById(R.id.tvSummaryTotalCost)).setText(String.format(Locale.US, "%.2f €", totalCost));
        ((TextView) findViewById(R.id.tvSummaryDrivingCost)).setText(String.format(Locale.US, "%.2f €", drivingCost));
        ((TextView) findViewById(R.id.tvSummaryStandbyCost)).setText(String.format(Locale.US, "%.2f €", standbyCost));
        
        String batteryStart = getIntent().getStringExtra("batteryStart");
        String batteryEnd = getIntent().getStringExtra("batteryEnd");
        ((TextView) findViewById(R.id.tvSummaryBatteryStart)).setText(batteryStart != null ? batteryStart : "--%");
        ((TextView) findViewById(R.id.tvSummaryBatteryEnd)).setText(batteryEnd != null ? batteryEnd : "--%");

        double co2Saved = 0.0;
        if (distance != null) {
            try {
                String numericPart = distance.replaceAll("[^0-9.,]", "").replace(",", ".");
                double km = Double.parseDouble(numericPart);
                co2Saved = km * 0.12;
            } catch (Exception ignored) {}
        }
        ((TextView) findViewById(R.id.tvSummaryCo2)).setText(getString(R.string.summary_co2_value, co2Saved));

        minitsClient = new MinitsApiClient(this);
        MinitsAuthenticator auth = new MinitsAuthenticator(this, minitsClient);
        auth.loadSavedAuth();
        minitsClient.setAuth(auth.getToken(), auth.getEmail());

        if (idReserva != -1) {
            fetchExtraDetails(idReserva, year, month);
        }

        findViewById(R.id.ivAtabalLogo).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(NetworkConstants.URL_FUNDACION_ATABAL));
            startActivity(intent);
        });

        findViewById(R.id.btnSummaryClose).setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.map = mapLibreMap;
        String styleUrl = getString(NetworkConstants.STYLE_LIBERTY);
        map.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
            if (!routePoints.isEmpty()) {
                drawRouteOnMap(style);
            }
        });
    }

    private void fetchExtraDetails(int idReserva, int year, int month) {
        CompletableFuture.runAsync(() -> {
            try {
                MinitsModels.DatosSQLRequest resReq = new MinitsModels.DatosSQLRequest("GEN_RESERVAS", Collections.singletonList("ID=" + idReserva));
                MinitsModels.GenericResponse<?> resRespRaw = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, resReq, MinitsModels.GenericResponse.class);
                
                if (resRespRaw != null && resRespRaw.data instanceof List<?> list && !list.isEmpty()) {
                    if (list.get(0) instanceof com.google.gson.internal.LinkedTreeMap<?, ?> mapData) {
                        Object bIni = mapData.get("BATERIA_INICIO");
                        Object bFin = mapData.get("BATERIA_FIN");
                        if (bIni != null && bFin != null) {
                            runOnUiThread(() -> {
                                ((TextView) findViewById(R.id.tvSummaryBatteryStart)).setText(String.format(Locale.US, "%.0f%%", Double.parseDouble(bIni.toString())));
                                ((TextView) findViewById(R.id.tvSummaryBatteryEnd)).setText(String.format(Locale.US, "%.0f%%", Double.parseDouble(bFin.toString())));
                            });
                        }
                    }
                }

                if (year != -1 && month != -1) {
                    String mesyear = String.format(Locale.US, "%02d%s", month, String.valueOf(year).substring(2));
                    String sql = "SELECT d.LATITUD, d.LONGITUD, 'viaje0.png' AS I " +
                                 "FROM gen_reservas r " +
                                 "LEFT OUTER JOIN datoscan" + mesyear + " d ON r.IDVEHICULO=d.IDVEHICULO " +
                                 "WHERE r.ID=" + idReserva + " AND d.FECHA>=r.FECHA_INI_SYS AND d.FECHA<=r.FECHA_FIN_SYS " +
                                 "ORDER BY d.FECHA";
                    
                    MinitsModels.DatosSQLRequest traceReq = new MinitsModels.DatosSQLRequest("MT_DatosMapaViaje", Collections.singletonList(sql));
                    
                    MinitsModels.TraceResponse traceResp = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, traceReq, MinitsModels.TraceResponse.class);
                    if (traceResp != null && traceResp.data != null && traceResp.data.length > 0) {
                        routePoints.clear();
                        for (MinitsModels.TracePoint p : traceResp.data) {
                            routePoints.add(new LatLng(p.LATITUD, p.LONGITUD));
                        }
                        
                        runOnUiThread(() -> {
                            if (map != null && map.getStyle() != null) {
                                drawRouteOnMap(map.getStyle());
                            }
                            findViewById(R.id.flSummaryHeader).setVisibility(View.GONE);
                            mapView.setVisibility(View.VISIBLE);
                        });
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching extra details", e);
            }
        });
    }

    private void drawRouteOnMap(@NonNull Style style) {
        if (routePoints.isEmpty()) return;

        List<Point> points = new ArrayList<>();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : routePoints) {
            points.add(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()));
            builder.include(latLng);
        }

        if (style.getSource(ROUTE_SOURCE_ID) == null) {
            style.addSource(new GeoJsonSource(ROUTE_SOURCE_ID, Feature.fromGeometry(LineString.fromLngLats(points))));
            LineLayer lineLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);
            lineLayer.setProperties(
                    PropertyFactory.lineColor(Color.RED),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            );
            style.addLayer(lineLayer);
        } else {
            GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);
            if (source != null) {
                source.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(points)));
            }
        }

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
