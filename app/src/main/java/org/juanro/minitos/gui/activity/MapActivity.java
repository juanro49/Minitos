package org.juanro.minitos.gui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;
import org.juanro.minitos.data.api.MinitsParser;
import org.juanro.minitos.data.api.OpenRouteServiceClient;
import org.juanro.minitos.data.api.config.NetworkConstants;
import org.juanro.minitos.model.entity.Vehicle;
import org.juanro.minitos.model.entity.VehicleWithFavorite;
import org.juanro.minitos.model.entity.Zone;
import org.juanro.minitos.service.TripNotificationService;
import org.juanro.minitos.viewmodel.VehiclesViewModel;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.offline.OfflineManager;
import org.maplibre.android.offline.OfflineRegion;
import org.maplibre.android.offline.OfflineRegionError;
import org.maplibre.android.offline.OfflineRegionStatus;
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.FillExtrusionLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonOptions;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;
import org.maplibre.geojson.Polygon;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.maplibre.android.style.expressions.Expression.all;
import static org.maplibre.android.style.expressions.Expression.eq;
import static org.maplibre.android.style.expressions.Expression.get;
import static org.maplibre.android.style.expressions.Expression.has;
import static org.maplibre.android.style.expressions.Expression.literal;

public class MapActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {
    private static final String TAG = "MapActivity";
    private static final String PREF_KEY_MAP_LAYER = "pref_map_layer";
    private static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";
    private static final String KEY_VEHICLE = "vehicle";
    private static final int tooOldUpdateDelay = 600000; //10 minutes

    private static final String VEHICLES_SOURCE_ID = "vehicles-source";
    private static final String VEHICLES_LAYER_ID = "vehicles-layer";
    private static final String CLUSTERS_LAYER_ID = "clusters-layer";
    private static final String CLUSTER_COUNT_LAYER_ID = "cluster-count-layer";
    private static final String ZONES_SOURCE_ID = "zones-source";
    private static final String ZONES_FILL_LAYER_ID = "zones-fill-layer";
    private static final String ZONES_OUTLINE_LAYER_ID = "zones-outline-layer";
    private static final String ROUTE_SOURCE_ID = "route-source";
    private static final String ROUTE_LAYER_ID = "route-layer";

    private static final String[] REQUEST_LOC_LIST = {
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    };
    private static final int REQUEST_LOC_PERMISSION_CODE = 1;

    private ProgressBar mProgressBar;
    private MapView mapView;
    private MapLibreMap map;
    private View cardDetail;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private boolean isDetailViewOpened = false;
    private ColorStateList defaultTextViewColors;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences settings;
    private long mDbLastUpdate;
    private boolean mAutoSelectedActiveTrip = false;
    private boolean mOfflineStarted = false;

    private LinearLayout minitsControls;
    private View llActiveReservationControls;
    private View minitsDetailsContainer;
    private TextView tvMinitsBatteryAutonomy;
    private TextView tvMinitsRateActive;
    private TextView tvMinitsRateStandby;
    private TextView tvMinitsSpecs;
    private Button btnReservar;
    private MaterialButton btnFavorite;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabRefresh;
    private MinitsApiClient minitsClient;
    private MinitsAuthenticator minitsAuth;
    private OpenRouteServiceClient orsClient;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private AppBarLayout mAppBarLayout;
    private VehiclesViewModel mViewModel;

    private List<VehicleWithFavorite> currentVehicles = new ArrayList<>();
    private List<Zone> currentZones = new ArrayList<>();
    private String selectedVehicleMatricula = null;
    private String currentFilter = "Todos";

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences, key) -> {
        if (PREF_KEY_MAP_LAYER.equals(key)) {
            String newStyleUrl = sharedPreferences.getString(PREF_KEY_MAP_LAYER, getString(R.string.pref_default_map_layer_value));
            if (map != null) {
                map.setStyle(new Style.Builder().fromUri(newStyleUrl), style -> {
                    setupVehiclesLayers(style);
                    setupZonesLayers(style);
                    setupRouteLayer(style);
                    setup3DBuildings(style);
                    enableLocationComponent(style);
                    updateVehiclesSource();
                    updateZonesSource();
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapLibre.getInstance(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mViewModel = new ViewModelProvider(this).get(VehiclesViewModel.class);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar mToolbar = findViewById(R.id.toolbar);
        mAppBarLayout = findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);

        mNavigationView = findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        
        minitsClient = new MinitsApiClient(this);
        minitsAuth = new MinitsAuthenticator(this, minitsClient);
        minitsAuth.loadSavedAuth();
        orsClient = new OpenRouteServiceClient(this);
        
        updateNavHeader();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, 
                R.string.app_name, R.string.app_name);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        mProgressBar = findViewById(R.id.map_progressbar);
        mProgressBar.getProgressDrawable().setColorFilter(
                new android.graphics.PorterDuffColorFilter(Color.parseColor("#FF7883"), android.graphics.PorterDuff.Mode.SRC_IN));
        mProgressBar.getIndeterminateDrawable().setColorFilter(
                new android.graphics.PorterDuffColorFilter(Color.parseColor("#FF7883"), android.graphics.PorterDuff.Mode.SRC_IN));
        mDbLastUpdate = settings.getLong(PREF_KEY_DB_LAST_UPDATE, -1);
        setDBLastUpdateText();

        fabMyLocation = findViewById(R.id.fab_my_location);
        fabRefresh = findViewById(R.id.fab_refresh);

        fabMyLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                moveToUserLocation();
            } else {
                ActivityCompat.requestPermissions(this,
                        REQUEST_LOC_LIST, REQUEST_LOC_PERMISSION_CODE);
            }
        });

        fabRefresh.setOnClickListener(v -> executeDownloadTask());

        btnFavorite = findViewById(R.id.btnFavorite);
        MaterialButton btnDirections = findViewById(R.id.btnDirections);
        
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnDirections.setOnClickListener(v -> triggerActionDirection());

        ViewCompat.setOnApplyWindowInsetsListener(mDrawerLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (mAppBarLayout != null) {
                mAppBarLayout.setPadding(0, insets.top, 0, 0);
            }
            View mainLayout = findViewById(R.id.activity_map_layout);
            if (mainLayout != null) {
                mainLayout.setPadding(0, 0, 0, insets.bottom);
            }
            
            View sheetContainer = findViewById(R.id.bottom_sheet_container);
            if (sheetContainer != null) {
                sheetContainer.setPadding(0, 0, 0, insets.bottom);
            }
            
            return WindowInsetsCompat.CONSUMED;
        });

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        cardDetail = findViewById(R.id.cardDetail);
        bottomSheetBehavior = BottomSheetBehavior.from(cardDetail);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    isDetailViewOpened = false;
                    selectedVehicleMatricula = null;
                    clearRoute();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                int height = bottomSheet.getHeight();
                int peekHeight = bottomSheetBehavior.getPeekHeight();
                int currentHeight = (int) (peekHeight + (height - peekHeight) * slideOffset);
                if (map != null) {
                    map.setPadding(0, 0, 0, currentHeight);
                }
            }
        });

        ChipGroup chipGroup = findViewById(R.id.chip_group_filters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chip_all) currentFilter = "Todos";
                else if (id == R.id.chip_carver) currentFilter = "Carver";
                else if (id == R.id.chip_twizy) currentFilter = "Twizy";
                else if (id == R.id.chip_patinete) currentFilter = "Patinete";
                updateVehiclesSource();
            }
        });

        minitsControls = findViewById(R.id.minitsControls);
        llActiveReservationControls = findViewById(R.id.llActiveReservationControls);
        minitsDetailsContainer = findViewById(R.id.minitsDetailsContainer);
        tvMinitsBatteryAutonomy = findViewById(R.id.tvMinitsBatteryAutonomy);
        tvMinitsRateActive = findViewById(R.id.tvMinitsRateActive);
        tvMinitsRateStandby = findViewById(R.id.tvMinitsRateStandby);
        tvMinitsSpecs = findViewById(R.id.tvMinitsSpecs);
        
        Button btnUnlock = findViewById(R.id.btnUnlock);
        Button btnLock = findViewById(R.id.btnLock);
        Button btnFinalizar = findViewById(R.id.btnFinalizar);
        btnReservar = findViewById(R.id.btnReservar);

        btnUnlock.setOnClickListener(v -> handleMinitsCommand("ON"));
        btnLock.setOnClickListener(v -> handleMinitsCommand("OFF"));
        btnReservar.setOnClickListener(v -> handleMinitsReservation());
        btnFinalizar.setOnClickListener(v -> handleMinitsFinalizar());

        mViewModel.getVehicles().observe(this, vehicles -> {
            currentVehicles = vehicles;
            updateVehiclesSource();
            mDbLastUpdate = settings.getLong(PREF_KEY_DB_LAST_UPDATE, -1);
            setDBLastUpdateText();
            
            String activeMat = settings.getString("active_reservation_matricula", null);
            if (activeMat != null) {
                String name = "Minitos";
                for (VehicleWithFavorite vwf : vehicles) {
                    if (vwf.getVehicle().getId().equals(activeMat)) {
                        name = vwf.getVehicle().getName();
                        break;
                    }
                }
                Intent serviceIntent = new Intent(this, TripNotificationService.class);
                serviceIntent.setAction(TripNotificationService.ACTION_START);
                serviceIntent.putExtra(TripNotificationService.EXTRA_VEHICLE_NAME, name);
                startService(serviceIntent);
            }
        });

        mViewModel.getZones().observe(this, zones -> {
            currentZones = zones;
            updateZonesSource();
        });
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.map = mapLibreMap;
        final String styleUrl = settings.getString(PREF_KEY_MAP_LAYER, getString(NetworkConstants.STYLE_LIBERTY));
        String finalUrl = styleUrl.startsWith("http") ? styleUrl : getString(NetworkConstants.STYLE_LIBERTY);
        
        map.setStyle(new Style.Builder().fromUri(finalUrl), style -> {
            setupVehiclesLayers(style);
            setupZonesLayers(style);
            setupRouteLayer(style);
            setup3DBuildings(style);
            enableLocationComponent(style);
            
            updateVehiclesSource();
            updateZonesSource();
            
            if (!checkIntentForVehicle() && !mAutoSelectedActiveTrip) {
                LatLng center = new LatLng(39.473, -6.371);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13));
                startOfflineDownload(center, finalUrl);
            }
        });

        map.addOnMapClickListener(latLng -> {
            PointF pixel = map.getProjection().toScreenLocation(latLng);
            List<Feature> features = map.queryRenderedFeatures(pixel, VEHICLES_LAYER_ID, CLUSTERS_LAYER_ID);

            if (!features.isEmpty()) {
                Feature feature = features.get(0);
                if (feature.hasProperty("point_count")) {
                    double zoom = map.getCameraPosition().zoom + 2;
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
                } else {
                    String matricula = feature.getStringProperty("matricula");
                    displayVehicleDetailsByMatricula(matricula);
                }
                return true;
            } else {
                closeDetailView();
                return false;
            }
        });
    }

    private void setupVehiclesLayers(@NonNull Style style) {
        GeoJsonOptions options = new GeoJsonOptions()
                .withCluster(true)
                .withClusterRadius(50)
                .withClusterMaxZoom(14);

        style.addSource(new GeoJsonSource(VEHICLES_SOURCE_ID, FeatureCollection.fromFeatures(new ArrayList<>()), options));

        CircleLayer clusters = new CircleLayer(CLUSTERS_LAYER_ID, VEHICLES_SOURCE_ID);
        clusters.setProperties(
                PropertyFactory.circleColor(Color.parseColor("#FF7883")),
                PropertyFactory.circleRadius(18f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(2f)
        );
        clusters.setFilter(has("point_count"));
        style.addLayer(clusters);

        SymbolLayer clusterCount = new SymbolLayer(CLUSTER_COUNT_LAYER_ID, VEHICLES_SOURCE_ID);
        clusterCount.setProperties(
                PropertyFactory.textField(Expression.toString(get("point_count"))),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(Color.WHITE),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textFont(new String[]{"Noto Sans Regular"})
        );
        style.addLayer(clusterCount);

        CircleLayer vehicleCircles = new CircleLayer("vehicle-circles", VEHICLES_SOURCE_ID);
        vehicleCircles.setProperties(
                PropertyFactory.circleColor(get("color")),
                PropertyFactory.circleRadius(14f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(2f)
        );
        vehicleCircles.setFilter(Expression.not(has("point_count")));
        style.addLayer(vehicleCircles);

        SymbolLayer vehicleLayer = new SymbolLayer(VEHICLES_LAYER_ID, VEHICLES_SOURCE_ID);
        vehicleLayer.setProperties(
                PropertyFactory.iconImage("vehicle-body-icon"),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconSize(0.5f),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
        );
        vehicleLayer.setFilter(Expression.not(has("point_count")));
        
        Drawable vehicleDrawable = ContextCompat.getDrawable(this, R.drawable.ic_vehicle_body);
        if (vehicleDrawable != null) {
            style.addImage("vehicle-body-icon", vehicleDrawable);
        }
        style.addLayer(vehicleLayer);

        SymbolLayer batteryLayer = new SymbolLayer("battery-layer", VEHICLES_SOURCE_ID);
        batteryLayer.setProperties(
                PropertyFactory.textField(Expression.concat(Expression.toString(get("battery")), literal("%"))),
                PropertyFactory.textSize(10f),
                PropertyFactory.textColor(Color.BLACK),
                PropertyFactory.textHaloColor(Color.WHITE),
                PropertyFactory.textHaloWidth(1f),
                PropertyFactory.textOffset(new Float[]{0f, -1.5f}),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                PropertyFactory.textFont(new String[]{"Noto Sans Regular"})
        );
        batteryLayer.setFilter(Expression.not(has("point_count")));
        style.addLayer(batteryLayer);
    }

    private void setupRouteLayer(@NonNull Style style) {
        style.addSource(new GeoJsonSource(ROUTE_SOURCE_ID));
        LineLayer routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);
        routeLayer.setProperties(
                PropertyFactory.lineColor(Color.parseColor("#4A90E2")),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineDasharray(new Float[]{2f, 1f}),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        );
        style.addLayerBelow(routeLayer, VEHICLES_LAYER_ID);
    }

    private void drawRouteToVehicle(double endLat, double endLon) {
        if (settings.getString("pref_ors_api_key", "").isEmpty()) {
            return;
        }
        LocationComponent locationComponent = map.getLocationComponent();
        if (locationComponent.isLocationComponentActivated() && locationComponent.getLastKnownLocation() != null) {
            Location myLoc = locationComponent.getLastKnownLocation();
            orsClient.getWalkingRoute(myLoc.getLatitude(), myLoc.getLongitude(), endLat, endLon, new OpenRouteServiceClient.Callback() {
                @Override
                public void onSuccess(String geoJson) {
                    runOnUiThread(() -> {
                        Style style = map.getStyle();
                        if (style != null) {
                            GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);
                            if (source != null) {
                                source.setGeoJson(geoJson);
                            }
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error drawing route", e);
                }
            });
        }
    }

    private void clearRoute() {
        if (map == null) return;
        Style style = map.getStyle();
        if (style != null) {
            GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);
            if (source != null) {
                source.setGeoJson(FeatureCollection.fromFeatures(new ArrayList<>()));
            }
        }
    }

    private void setup3DBuildings(@NonNull Style style) {
        if (style.getSource("openmaptiles") != null) {
            FillExtrusionLayer fillExtrusionLayer = new FillExtrusionLayer("3d-buildings", "openmaptiles");
            fillExtrusionLayer.setSourceLayer("building");
            fillExtrusionLayer.setFilter(eq(get("hide_3d"), false));
            fillExtrusionLayer.setProperties(
                    PropertyFactory.fillExtrusionColor(Color.LTGRAY),
                    PropertyFactory.fillExtrusionHeight(get("render_height")),
                    PropertyFactory.fillExtrusionBase(get("render_min_height")),
                    PropertyFactory.fillExtrusionOpacity(0.6f)
            );
            fillExtrusionLayer.setMinZoom(16f);
            style.addLayer(fillExtrusionLayer);
        }
    }

    private void setupZonesLayers(@NonNull Style style) {
        style.addSource(new GeoJsonSource(ZONES_SOURCE_ID, FeatureCollection.fromFeatures(new ArrayList<>())));

        FillLayer fillLayer = new FillLayer(ZONES_FILL_LAYER_ID, ZONES_SOURCE_ID);
        fillLayer.setProperties(
                PropertyFactory.fillColor(get("fillColor")),
                PropertyFactory.fillOpacity(get("opacity"))
        );
        
        if (style.getLayer("water") != null) {
            style.addLayerAbove(fillLayer, "water");
        } else {
            style.addLayerAt(fillLayer, 1);
        }

        LineLayer outlineLayer = new LineLayer(ZONES_OUTLINE_LAYER_ID, ZONES_SOURCE_ID);
        outlineLayer.setProperties(
                PropertyFactory.lineColor(get("color")),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineOpacity(1.0f)
        );
        
        String topRoadLayer = null;
        for (org.maplibre.android.style.layers.Layer layer : style.getLayers()) {
            if (layer.getId().contains("road") || layer.getId().contains("bridge")) {
                topRoadLayer = layer.getId();
            }
        }
        
        if (topRoadLayer != null) {
            style.addLayerAbove(outlineLayer, topRoadLayer);
        } else {
            style.addLayerAt(outlineLayer, style.getLayers().size());
        }
    }

    private void updateVehiclesSource() {
        if (map == null) return;
        Style style = map.getStyle();
        if (style == null || !style.isFullyLoaded()) return;
        GeoJsonSource source = style.getSourceAs(VEHICLES_SOURCE_ID);
        if (source == null) return;

        List<Feature> features = new ArrayList<>();
        String activeMatricula = settings.getString("active_reservation_matricula", null);
        Feature activeFeature = null;

        for (VehicleWithFavorite vwf : currentVehicles) {
            Vehicle v = vwf.getVehicle();
            
            if (!currentFilter.equals("Todos")) {
                if (v.getExtraData() != null) {
                    try {
                        org.json.JSONObject extra = new org.json.JSONObject(v.getExtraData());
                        String modelo = extra.optString("modelo", "");
                        if (!modelo.contains(currentFilter)) continue;
                    } catch (Exception ignored) {}
                }
            }

            Point point = Point.fromLngLat(v.getLongitude(), v.getLatitude());
            Feature feature = Feature.fromGeometry(point);
            feature.addStringProperty("matricula", v.getId());
            feature.addStringProperty("name", v.getName());
            feature.addBooleanProperty("isFavorite", vwf.isFavorite());
            
            String color = "#FF7140";
            String estado = "LIBRE";
            String batteryStr = "0";
            try {
                if (v.getExtraData() != null) {
                    org.json.JSONObject extra = new org.json.JSONObject(v.getExtraData());
                    color = extra.optString("color", color);
                    estado = extra.optString("estado", "LIBRE");
                    String b = extra.optString("bateria", "0");
                    batteryStr = b.replace("%", "").trim();
                }
            } catch (Exception ignored) {}
            
            if (!"LIBRE".equals(estado)) {
                color = "#808080";
            }
            feature.addStringProperty("color", color);
            try {
                feature.addNumberProperty("battery", Double.parseDouble(batteryStr));
            } catch (Exception e) {
                feature.addNumberProperty("battery", 0);
            }
            
            features.add(feature);

            if (!mAutoSelectedActiveTrip && activeMatricula != null && activeMatricula.equals(v.getId())) {
                activeFeature = feature;
            }
        }

        Log.d(TAG, "Updating vehicles source with " + features.size() + " features");
        source.setGeoJson(FeatureCollection.fromFeatures(features));

        if (activeFeature != null) {
            mAutoSelectedActiveTrip = true;
            final String mat = activeMatricula;
            Point p = (Point) activeFeature.geometry();
            if (p != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(p.latitude(), p.longitude()), 16));
                displayVehicleDetailsByMatricula(mat);
            }
        }
    }

    private void updateZonesSource() {
        if (map == null) return;
        Style style = map.getStyle();
        if (style == null || !style.isFullyLoaded()) return;
        
        GeoJsonSource source = style.getSourceAs(ZONES_SOURCE_ID);
        if (source == null) return;

        List<Feature> features = new ArrayList<>();
        Gson gson = new Gson();

        for (Zone zone : currentZones) {
            try {
                MinitsModels.ZonePoint[] points = gson.fromJson(zone.getPoints(), MinitsModels.ZonePoint[].class);
                if (points != null && points.length > 2) {
                    List<Point> polygonPoints = new ArrayList<>();
                    for (MinitsModels.ZonePoint pt : points) {
                        polygonPoints.add(Point.fromLngLat(Double.parseDouble(pt.longitude), Double.parseDouble(pt.latitude)));
                    }
                    if (!polygonPoints.get(0).equals(polygonPoints.get(polygonPoints.size() - 1))) {
                        polygonPoints.add(polygonPoints.get(0));
                    }
                    
                    List<List<Point>> coordinates = new ArrayList<>();
                    coordinates.add(polygonPoints);
                    
                    Feature feature = Feature.fromGeometry(Polygon.fromLngLats(coordinates));
                    
                    String fillColor = zone.getFillColor() != null ? zone.getFillColor() : "#88FF7883";
                    float opacity = 0.6f;
                    
                    if (fillColor.startsWith("#") && fillColor.length() == 9) {
                        try {
                            String alphaHex = fillColor.substring(1, 3);
                            opacity = Math.min(0.95f, (Integer.parseInt(alphaHex, 16) / 255f) * 2.5f);
                            fillColor = "#" + fillColor.substring(3);
                        } catch (Exception ignored) {}
                    }
                    
                    feature.addStringProperty("fillColor", fillColor);
                    feature.addNumberProperty("opacity", opacity);
                    
                    String borderColor = zone.getColor() != null ? zone.getColor() : "#FF7883";
                    feature.addStringProperty("color", borderColor);
                    features.add(feature);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing zone for GeoJSON", e);
            }
        }
        Log.d(TAG, "Updating zones source with " + features.size() + " features");
        source.setGeoJson(FeatureCollection.fromFeatures(features));
    }

    private void displayVehicleDetailsByMatricula(String matricula) {
        selectedVehicleMatricula = matricula;
        for (VehicleWithFavorite vwf : currentVehicles) {
            if (vwf.getVehicle().getId().equals(matricula)) {
                setVehicleDetails(vwf.getVehicle());
                if (vwf.isFavorite()) {
                    btnFavorite.setIconResource(R.drawable.ic_menu_favorite);
                } else {
                    btnFavorite.setIconResource(R.drawable.ic_menu_favorite_outline);
                }

                drawRouteToVehicle(vwf.getVehicle().getLatitude(), vwf.getVehicle().getLongitude());
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                isDetailViewOpened = true;
                break;
            }
        }
    }

    private void closeDetailView() {
        if (isDetailViewOpened) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            isDetailViewOpened = false;
            selectedVehicleMatricula = null;
            clearRoute();
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private boolean checkIntentForVehicle() {
        if (getIntent().hasExtra(KEY_VEHICLE)) {
            Vehicle vehicleExtra;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                vehicleExtra = getIntent().getSerializableExtra(KEY_VEHICLE, Vehicle.class);
            } else {
                //noinspection deprecation
                vehicleExtra = (Vehicle) getIntent().getSerializableExtra(KEY_VEHICLE);
            }

            if (vehicleExtra != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(vehicleExtra.getLatitude(), vehicleExtra.getLongitude()), 16));
                displayVehicleDetailsByMatricula(vehicleExtra.getId());
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("MissingPermission")
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationComponent locationComponent = map.getLocationComponent();
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, loadedMapStyle).build());
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }
    }

    private void moveToUserLocation() {
        LocationComponent locationComponent = map.getLocationComponent();
        if (locationComponent.isLocationComponentActivated() && locationComponent.getLastKnownLocation() != null) {
            Location loc = locationComponent.getLastKnownLocation();
            map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(loc.getLatitude(), loc.getLongitude())));
        } else {
            Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_LONG).show();
        }
    }

    private void triggerActionDirection() {
        if (selectedVehicleMatricula == null) return;
        
        Vehicle selectedVehicle = null;
        for (VehicleWithFavorite vwf : currentVehicles) {
            if (vwf.getVehicle().getId().equals(selectedVehicleMatricula)) {
                selectedVehicle = vwf.getVehicle();
                break;
            }
        }
        
        if (selectedVehicle == null) return;
        
        Uri sLocationUri = Uri.parse("geo:" + selectedVehicle.getLatitude() + "," + selectedVehicle.getLongitude());
        Intent intent = new Intent(Intent.ACTION_VIEW, sLocationUri);
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            activities = packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0));
        } else {
            activities = packageManager.queryIntentActivities(intent, 0);
        }
        if (!activities.isEmpty()) {
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.no_nav_application), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleFavorite() {
        if (selectedVehicleMatricula == null) return;
        
        final String matricula = selectedVehicleMatricula;
        mViewModel.isFavorite(matricula).observe(this, isFav -> {
            mViewModel.toggleFavorite(matricula, !isFav);
            Toast.makeText(MapActivity.this,
                    !isFav ? getString(R.string.vehicle_added_to_favorites) : getString(R.string.vehicles_removed_from_favorites), Toast.LENGTH_SHORT).show();
            
            btnFavorite.setIconResource(!isFav ? R.drawable.ic_menu_favorite : R.drawable.ic_menu_favorite_outline);
            for (VehicleWithFavorite vwf : currentVehicles) {
                if (vwf.getVehicle().getId().equals(matricula)) {
                    vwf.setFavorite(!isFav);
                    break;
                }
            }
        });
    }

    private void setVehicleDetails(Vehicle markerVehicle) {
        TextView vehicleName = findViewById(R.id.vehicleName);
        View llWalkingInfo = findViewById(R.id.llWalkingInfo);
        TextView tvWalkingTimeDistance = findViewById(R.id.tvWalkingTimeDistance);

        vehicleName.setText(markerVehicle.getName());
        setLastUpdateText(markerVehicle.getLastUpdate());
        
        // Calculate distance and walking time
        LocationComponent locationComponent = (map != null) ? map.getLocationComponent() : null;
        if (locationComponent != null && locationComponent.isLocationComponentActivated() && locationComponent.getLastKnownLocation() != null) {
            Location myLoc = locationComponent.getLastKnownLocation();
            float[] results = new float[1];
            Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), markerVehicle.getLatitude(), markerVehicle.getLongitude(), results);
            float distanceInMeters = results[0];
            
            int minutes = (int) Math.ceil(distanceInMeters / 80.0); // Approx 5km/h = 83m/min
            String distText;
            if (distanceInMeters >= 1000) {
                distText = String.format(Locale.US, "%.2f km", distanceInMeters / 1000.0);
            } else {
                distText = String.format(Locale.US, "%d m", (int) distanceInMeters);
            }
            
            tvWalkingTimeDistance.setText(getString(R.string.walking_time_distance, minutes, distText));
            llWalkingInfo.setVisibility(View.VISIBLE);
        } else {
            llWalkingInfo.setVisibility(View.GONE);
        }

        boolean isMinits = MinitsParser.MINITS_NETWORK_ID.equals(markerVehicle.getNetworkId());
        
        if (isMinits) {
            minitsDetailsContainer.setVisibility(View.VISIBLE);
            
            String activeResId = settings.getString("active_reservation_id", null);
            String activeMatricula = settings.getString("active_reservation_matricula", null);
            boolean hasActiveRes = activeResId != null && markerVehicle.getId().equals(activeMatricula);
            
            if (hasActiveRes) {
                btnReservar.setVisibility(View.GONE);
                llActiveReservationControls.setVisibility(View.VISIBLE);
            } else {
                btnReservar.setVisibility(View.VISIBLE);
                llActiveReservationControls.setVisibility(View.GONE);
            }

            String extraData = markerVehicle.getExtraData();
            if (extraData != null) {
                try {
                    org.json.JSONObject extra = new org.json.JSONObject(extraData);
                    tvMinitsBatteryAutonomy.setText(getString(R.string.vehicle_battery_autonomy, 
                            extra.optString("bateria", "--%"), 
                            extra.optString("autonomia", "-- Km")));
                    
                    tvMinitsRateActive.setText(getString(R.string.vehicle_rate_active, 
                            extra.optDouble("tarifa_activa", 0)));
                    
                    tvMinitsRateStandby.setText(getString(R.string.vehicle_rate_standby, 
                            extra.optDouble("tarifa_standby", 0)));
                    
                    tvMinitsSpecs.setText(getString(R.string.vehicle_specs,
                            extra.optInt("pasajeros", 0),
                            extra.optString("motor", "-"),
                            extra.optString("transmision", "-")));
                    
                    String modelo = extra.optString("modelo", "");
                    if (!modelo.isEmpty()) {
                        String title = markerVehicle.getId() + " - " + modelo;
                        vehicleName.setText(title);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Minits extra data", e);
                }
            }
        } else {
            minitsDetailsContainer.setVisibility(View.GONE);
        }

        TextView vehicleNetwork = findViewById(R.id.vehicleNetwork);
        vehicleNetwork.setText(isMinits ? "Minits" : markerVehicle.getNetworkId());

        TextView vehicleAddress = findViewById(R.id.vehicleAddress);
        if (markerVehicle.getAddress() != null) {
            vehicleAddress.setText(markerVehicle.getAddress());
            vehicleAddress.setVisibility(View.VISIBLE);
        } else {
            vehicleAddress.setVisibility(View.GONE);
        }
    }

    private void executeDownloadTask() {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(true);

        double lat = 39.473; 
        double lon = -6.371;
        
        LocationComponent locationComponent = (map != null) ? map.getLocationComponent() : null;
        Location myLoc = (locationComponent != null && locationComponent.isLocationComponentActivated()) ? locationComponent.getLastKnownLocation() : null;
        
        if (myLoc != null) {
            lat = myLoc.getLatitude();
            lon = myLoc.getLongitude();
        } else {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            try {
                Location lastLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLoc == null) {
                    lastLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastLoc != null) {
                    lat = lastLoc.getLatitude();
                    lon = lastLoc.getLongitude();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission denied: " + e.getMessage());
            }
        }

        mViewModel.syncVehicles(lat, lon);
        new Handler(Looper.getMainLooper()).postDelayed(() -> mProgressBar.setVisibility(View.GONE), 3000);
    }

    private class LastUpdateRunnable implements Runnable {
        private final String rawLastUpdateISO8601;
        private final SimpleDateFormat timestampFormatISO8601;
        private final TextView vehicleLastUpdate;

        public LastUpdateRunnable(String lastUpdateText) {
            rawLastUpdateISO8601 = lastUpdateText;
            timestampFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            timestampFormatISO8601.setTimeZone(TimeZone.getTimeZone("UTC"));
            vehicleLastUpdate = findViewById(R.id.vehicleLastUpdate);
            if(defaultTextViewColors == null) {
                defaultTextViewColors = vehicleLastUpdate.getTextColors();
            }
        }

        public void run() {
            try {
                java.util.Date date = timestampFormatISO8601.parse(rawLastUpdateISO8601);
                if (date == null) return;
                long lastUpdate = date.getTime();
                long currentDateTime = System.currentTimeMillis();
                long timeDiff = (currentDateTime - lastUpdate) / 1000;

                if (timeDiff < 60) {
                    vehicleLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_secondes_ago, (int) timeDiff, (int) timeDiff));
                    mHandler.postDelayed(this, 1000);
                } else if (timeDiff < 3600) {
                    int minutes = (int) timeDiff / 60;
                    vehicleLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_minutes_ago, minutes, minutes));
                    mHandler.postDelayed(this, 1000);
                } else if (timeDiff < 86400) {
                    int hours = (int) timeDiff / 3600;
                    vehicleLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_hours_ago, hours, hours));
                    mHandler.postDelayed(this, 60000);
                } else {
                    int days = (int) timeDiff / 86400;
                    vehicleLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_days_ago, days, days));
                }

                if (mDbLastUpdate - lastUpdate > tooOldUpdateDelay) {
                    vehicleLastUpdate.setTextColor(Color.RED);
                } else if (defaultTextViewColors != null) {
                    vehicleLastUpdate.setTextColor(defaultTextViewColors.getDefaultColor());
                }
            } catch (Exception e) {
                Log.w(TAG, "Error updating last update text", e);
            }
        }
    }

    private void setLastUpdateText(String rawLastUpdateISO8601) {
        mHandler.removeCallbacksAndMessages(null);
        if (rawLastUpdateISO8601 != null && !rawLastUpdateISO8601.isEmpty()) {
            mHandler.post(new LastUpdateRunnable(rawLastUpdateISO8601));
        }
    }

    private void setDBLastUpdateText() {
        TextView lastUpdate = findViewById(R.id.mapDbLastUpdate);
        if (mDbLastUpdate == -1) {
            lastUpdate.setText(getString(R.string.db_last_update, getString(R.string.db_last_update_never)));
        } else {
            lastUpdate.setText(getString(R.string.db_last_update, DateUtils.formatSameDayTime(mDbLastUpdate, System.currentTimeMillis(), DateFormat.DEFAULT, DateFormat.DEFAULT).toString()));
        }
    }

    private void handleMinitsCommand(final String type) {
        minitsAuth.checkSession(new MinitsApiClient.Callback<MinitsModels.AccountStatusResponse>() {
            @Override
            public void onSuccess(MinitsModels.AccountStatusResponse response) {
                if (response != null && !response.hasError) {
                    executeCommand(type);
                } else {
                    handleAuthError();
                }
            }

            @Override
            public void onError(Exception e) {
                executeCommand(type);
            }
        });
    }

    private void executeCommand(final String type) {
        if (!minitsAuth.isLoggedIn()) {
            Toast.makeText(this, "Inicie sesión", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MinitosProfileActivity.class));
            return;
        }
        final String resId = settings.getString("active_reservation_id", null);
        if (resId == null) {
            Toast.makeText(this, "Sin reserva activa", Toast.LENGTH_SHORT).show();
            return;
        }

        final String matricula = selectedVehicleMatricula;
        if (matricula == null) return;
        
        Vehicle selectedVehicle = null;
        for (VehicleWithFavorite vwf : currentVehicles) {
            if (vwf.getVehicle().getId().equals(matricula)) {
                selectedVehicle = vwf.getVehicle();
                break;
            }
        }
        if (selectedVehicle == null) return;

        final Vehicle vehicle = selectedVehicle;
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(true);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("tipo", type);
                    body.put("usuario", minitsAuth.getEmail());
                    body.put("matricula", vehicle.getId());
                    body.put("numeroreserva", resId);
                    body.put("latitude", vehicle.getLatitude());
                    body.put("longitude", vehicle.getLongitude());

                    MinitsModels.GenericResponse<?> response = minitsClient.post(NetworkConstants.PATH_CONDUCIR, body, MinitsModels.GenericResponse.class);

                    if (type.equals("ON") && response != null && !response.hasError) {
                        Thread.sleep(2000);
                        MinitsModels.DatosSQLRequest checkReq = new MinitsModels.DatosSQLRequest("VehiculoArrancado", Collections.singletonList(resId));
                        MinitsModels.VehiculoArrancadoResponse checkResp = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, checkReq, MinitsModels.VehiculoArrancadoResponse.class);
                        if (checkResp != null && checkResp.data != null && checkResp.data.length > 0) {
                            final int started = checkResp.data[0].ARRANCADO;
                            runOnUiThread(() -> Toast.makeText(MapActivity.this, started == 1 ? "Motor ARRANCADO" : "Motor APAGADO", Toast.LENGTH_SHORT).show());
                        }
                    }

                    if (response != null && response.hasError && "F0".equals(response.message)) {
                        runOnUiThread(() -> {
                            mProgressBar.setVisibility(View.GONE);
                            Toast.makeText(MapActivity.this, "ERROR: Freno de mano requerido", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    runOnUiThread(() -> {
                        mProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MapActivity.this, type.equals("ON") ? "Vehículo desbloqueado" : "Vehículo bloqueado", Toast.LENGTH_SHORT).show();
                    });
                } catch (org.juanro.minitos.data.api.MinitsAuthException e) {
                    handleAuthError();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        mProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MapActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void handleMinitsReservation() {
        if (!minitsAuth.isLoggedIn()) {
            startActivity(new Intent(this, MinitosProfileActivity.class));
            return;
        }
        
        final String matricula = selectedVehicleMatricula;
        if (matricula == null) return;
        
        Vehicle selectedVehicle = null;
        for (VehicleWithFavorite vwf : currentVehicles) {
            if (vwf.getVehicle().getId().equals(matricula)) {
                selectedVehicle = vwf.getVehicle();
                break;
            }
        }
        if (selectedVehicle == null) return;
        
        final Vehicle vehicle = selectedVehicle;
        mProgressBar.setVisibility(View.VISIBLE);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("usuario", minitsAuth.getEmail());
                    body.put("matricula", vehicle.getId());
                    body.put("TARIFADIARIA", 0);

                    MinitsModels.ReservationResponse response = minitsClient.post(NetworkConstants.PATH_RESERVAR, body, MinitsModels.ReservationResponse.class);
                    if (response != null && response.data != null) {
                        settings.edit().putString("active_reservation_id", response.data.NUMERORESERVA).putString("active_reservation_matricula", vehicle.getId()).apply();
                        
                        Intent serviceIntent = new Intent(MapActivity.this, TripNotificationService.class);
                        serviceIntent.setAction(TripNotificationService.ACTION_START);
                        serviceIntent.putExtra(TripNotificationService.EXTRA_VEHICLE_NAME, vehicle.getName());
                        startService(serviceIntent);

                        runOnUiThread(() -> {
                            mProgressBar.setVisibility(View.GONE);
                            Toast.makeText(MapActivity.this, "Reservado", Toast.LENGTH_SHORT).show();
                            setVehicleDetails(vehicle);
                        });
                    }
                } catch (org.juanro.minitos.data.api.MinitsAuthException e) {
                    handleAuthError();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        mProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MapActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void handleMinitsFinalizar() {
        final String resId = settings.getString("active_reservation_id", null);
        final String matricula = settings.getString("active_reservation_matricula", null);
        if (resId == null) return;

        mProgressBar.setVisibility(View.VISIBLE);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("usuario", minitsAuth.getEmail());
                    body.put("NUMERORESERVA", resId);
                    body.put("MATRICULA", matricula);
                    body.put("VERSION", 2);

                    MinitsModels.FinalizarConduccionResponse response = minitsClient.post(NetworkConstants.PATH_FINALIZAR, body, MinitsModels.FinalizarConduccionResponse.class);
                    settings.edit().remove("active_reservation_id").remove("active_reservation_matricula").apply();
                    
                    Intent serviceIntent = new Intent(MapActivity.this, TripNotificationService.class);
                    serviceIntent.setAction(TripNotificationService.ACTION_STOP);
                    startService(serviceIntent);

                    final MinitsModels.FinalizarConduccionData tripData = (response != null) ? response.data : null;

                    runOnUiThread(() -> {
                        mProgressBar.setVisibility(View.GONE);
                        Intent intent = new Intent(MapActivity.this, MinitosPhotoActivity.class);
                        intent.putExtra("matricula", matricula);
                        intent.putExtra("reservationId", resId);
                        if (tripData != null) {
                            intent.putExtra("totalCost", tripData.COSTETOTAL);
                            intent.putExtra("initialCost", tripData.COSTE_INICIAL);
                            intent.putExtra("standbyCost", tripData.COSTE_STANDBY);
                            intent.putExtra("drivingCost", tripData.COSTE_CONDUCCION);
                            intent.putExtra("batteryStart", String.format(Locale.US, "%.0f%%", tripData.BATERIA_INICIO));
                            intent.putExtra("batteryEnd", String.format(Locale.US, "%.0f%%", tripData.BATERIA_FIN));
                            intent.putExtra("distance", String.format(Locale.US, "%.2f Km", tripData.DISTANCIA));
                            intent.putExtra("dateRange", tripData.HORA_INICIO + " - " + tripData.HORA_FIN);
                        }
                        startActivity(intent);
                        if (selectedVehicleMatricula != null) {
                             for(VehicleWithFavorite vwf : currentVehicles) {
                                 if(vwf.getVehicle().getId().equals(selectedVehicleMatricula)) {
                                     setVehicleDetails(vwf.getVehicle());
                                     break;
                                 }
                             }
                        }
                    });
                } catch (org.juanro.minitos.data.api.MinitsAuthException e) {
                    handleAuthError();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        mProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MapActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void handleAuthError() {
        runOnUiThread(() -> {
            mProgressBar.setVisibility(View.GONE);
            minitsAuth.logout();
            Toast.makeText(MapActivity.this, "Sesión expirada", Toast.LENGTH_LONG).show();
            startActivity(new Intent(MapActivity.this, MinitosProfileActivity.class));
            invalidateOptionsMenu();
        });
    }

    private void updateNavHeader() {
        View headerView = mNavigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvName = headerView.findViewById(R.id.tvHeaderName);
            TextView tvEmail = headerView.findViewById(R.id.tvHeaderEmail);
            if (minitsAuth.isLoggedIn()) {
                if (tvName != null) tvName.setText(minitsAuth.getName());
                if (tvEmail != null) tvEmail.setText(minitsAuth.getEmail());
            } else {
                if (tvName != null) tvName.setText(R.string.all_vehicles);
                if (tvEmail != null) tvEmail.setText(R.string.no_vehicles);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        mDrawerLayout.closeDrawers();
        int id = item.getItemId();
        
        if (id == R.id.nav_vehicles) {
            startActivity(new Intent(this, VehiclesListActivity.class));
        } else if (id == R.id.nav_wallet) {
            startActivity(new Intent(this, MinitosWalletActivity.class));
        } else if (id == R.id.nav_trips) {
            startActivity(new Intent(this, MinitosHistoryActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, MinitosProfileActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, MinitosHelpActivity.class));
        }
        return true;
    }

    private void startOfflineDownload(LatLng center, final String styleUrl) {
        if (mOfflineStarted) return;
        mOfflineStarted = true;

        OfflineManager offlineManager = OfflineManager.getInstance(this);
        org.maplibre.android.geometry.LatLngBounds bounds = new org.maplibre.android.geometry.LatLngBounds.Builder()
                .include(new LatLng(center.getLatitude() + 0.05, center.getLongitude() + 0.05))
                .include(new LatLng(center.getLatitude() - 0.05, center.getLongitude() - 0.05))
                .build();

        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                styleUrl, bounds, 10, 16, getResources().getDisplayMetrics().density);

        byte[] metadata;
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject();
            jsonObject.put("region_name", "Sede Area");
            metadata = jsonObject.toString().getBytes("UTF-8");
        } catch (Exception e) {
            metadata = new byte[0];
        }

        offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {
            @Override
            public void onCreate(OfflineRegion offlineRegion) {
                offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
                offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
                    @Override
                    public void onStatusChanged(OfflineRegionStatus status) {
                        if (status.isComplete()) {
                            Log.d(TAG, "Offline region download complete");
                        }
                    }

                    @Override
                    public void onError(OfflineRegionError error) {
                        Log.e(TAG, "Offline download error: " + error.getMessage());
                    }

                    @Override
                    public void mapboxTileCountLimitExceeded(long limit) {
                        Log.w(TAG, "Offline tile limit exceeded");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error creating offline region: " + error);
            }
        });
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
        updateNavHeader();
        long currentTime = System.currentTimeMillis();
        if ((mDbLastUpdate != -1) && ((currentTime - mDbLastUpdate) > 600000)) {
            executeDownloadTask();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        mHandler.removeCallbacksAndMessages(null);
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
        if (settings != null) {
            settings.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
