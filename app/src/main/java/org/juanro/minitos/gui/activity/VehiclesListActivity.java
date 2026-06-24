package org.juanro.minitos.gui.activity;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.gui.adapter.SearchVehicleAdapter;
import org.juanro.minitos.gui.fragment.VehiclesListFragment;
import org.juanro.minitos.model.entity.Vehicle;
import org.juanro.minitos.model.entity.VehicleWithFavorite;
import org.juanro.minitos.util.Preferences;
import org.juanro.minitos.viewmodel.VehiclesViewModel;

import java.text.DateFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Objects;

public class VehiclesListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREF_KEY_DEFAULT_TAB = "pref_default_tab";

    private static final String[] REQUEST_LOC_LIST = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int REQUEST_LOC_CODE = 1;

    private ArrayList<VehicleWithFavorite> nearbyVehicles;
    private ArrayList<VehicleWithFavorite> vehicles;
    private ArrayList<VehicleWithFavorite> favVehicles;
    private VehiclesViewModel vehiclesViewModel;

    private SharedPreferences settings;

    private SearchView searchView;
    private TabsPagerAdapter tabsPagerAdapter;

    private VehiclesListFragment allVehiclesFragment;
    private VehiclesListFragment favoriteVehiclesFragment;
    private VehiclesListFragment nearbyVehiclesFragment;
    private ProgressBar mProgressBar;

    private SwipeRefreshLayout refreshLayout;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private MinitsAuthenticator minitsAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicles_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mNavigationView = findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        org.juanro.minitos.data.api.MinitsApiClient minitsClient = new org.juanro.minitos.data.api.MinitsApiClient(this);
        minitsAuth = new org.juanro.minitos.data.api.MinitsAuthenticator(this, minitsClient);
        minitsAuth.loadSavedAuth();
        updateNavHeader();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        View root = findViewById(R.id.activity_vehicles_layout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBarLayout.setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        refreshLayout = findViewById(R.id.swipe_container);
        refreshLayout.setColorSchemeResources(R.color.bike_red, R.color.parking_blue_dark);
        refreshLayout.setOnRefreshListener(this::executeDownloadTask);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(2);

        TabLayout tabLayout = findViewById(R.id.tabLayout);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    if (ContextCompat.checkSelfPermission(VehiclesListActivity.this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(VehiclesListActivity.this,
                                REQUEST_LOC_LIST, REQUEST_LOC_CODE);
                    } else {
                        setNearbyVehicles();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                refreshLayout.setEnabled(state == ViewPager2.SCROLL_STATE_IDLE);
            }
        });

        mProgressBar = findViewById(R.id.home_progressbar);
        mProgressBar.getProgressDrawable().setColorFilter(
                new android.graphics.PorterDuffColorFilter(Color.parseColor("#FF7883"), android.graphics.PorterDuff.Mode.SRC_IN));
        mProgressBar.getIndeterminateDrawable().setColorFilter(
                new android.graphics.PorterDuffColorFilter(Color.parseColor("#FF7883"), android.graphics.PorterDuff.Mode.SRC_IN));

        vehiclesViewModel = new ViewModelProvider(this).get(VehiclesViewModel.class);
        vehicles = new ArrayList<>();
        favVehicles = new ArrayList<>();
        nearbyVehicles = new ArrayList<>();

        vehiclesViewModel.getVehicles().observe(this, list -> {
            if (list != null) {
                vehicles.clear();
                vehicles.addAll(list);
                getPagerAdapter().updateAllVehiclesListFragment(vehicles);
                setNearbyVehicles();
                setDBLastUpdateText();
            }
        });

        vehiclesViewModel.getFavoriteVehicles().observe(this, list -> {
            if (list != null) {
                favVehicles.clear();
                favVehicles.addAll(list);
                getPagerAdapter().updateFavoriteVehiclesFragment(favVehicles);
            }
        });

        tabsPagerAdapter = new TabsPagerAdapter(this);
        viewPager.setAdapter(tabsPagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabsPagerAdapter.getPageTitle(position))).attach();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        int defaultTabIndex = Integer.parseInt(settings.getString(PREF_KEY_DEFAULT_TAB, "0"));
        viewPager.setCurrentItem(defaultTabIndex);

        setDBLastUpdateText();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOC_CODE) {
            if (grantResults.length == 2 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setNearbyVehicles();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                nearbyVehiclesFragment.setEmptyView(R.string.location_not_granted);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavHeader();
        long dbLastUpdate = settings.getLong(Preferences.KEY_DB_LAST_UPDATE, -1);
        long currentTime = System.currentTimeMillis();

        if ((dbLastUpdate != -1) && ((currentTime - dbLastUpdate) > 600000)) {
            executeDownloadTask();
        }
    }

    private void setDBLastUpdateText() {
        TextView lastUpdate = findViewById(R.id.dbLastUpdate);
        long dbLastUpdate = settings.getLong(Preferences.KEY_DB_LAST_UPDATE, -1);

        if (dbLastUpdate == -1) {
            lastUpdate.setText(getString(R.string.db_last_update, getString(R.string.db_last_update_never)));
        } else {
            lastUpdate.setText(getString(R.string.db_last_update, DateUtils.formatSameDayTime(dbLastUpdate, System.currentTimeMillis(), DateFormat.DEFAULT, DateFormat.DEFAULT)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vehicles_list, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        SearchableInfo searchableInfo = manager.getSearchableInfo(getComponentName());
        if (searchableInfo != null) {
            searchView.setSearchableInfo(searchableInfo);
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                loadData(s);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private String normalize(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+|\\s|'|-|_", "").toLowerCase();
    }

    private void loadData(String query) {
        ArrayList<VehicleWithFavorite> queryVehicles = new ArrayList<>();
        String[] columns = new String[]{"_id", "text"};
        Object[] temp = new Object[]{0, "default"};

        MatrixCursor cursor = new MatrixCursor(columns);

        if (vehicles != null) {
            for (int i = 0; i < vehicles.size(); i++) {
                VehicleWithFavorite vehicleWithFavorite = vehicles.get(i);
                Vehicle vehicle = vehicleWithFavorite.getVehicle();
                String normalizedQuery = normalize(query.toLowerCase());
                if (normalize(vehicle.getName().toLowerCase()).contains(normalizedQuery) ||
                        normalize(vehicle.getId().toLowerCase()).contains(normalizedQuery)) {
                    temp[0] = i;
                    temp[1] = vehicle.getName() + " (" + vehicle.getId() + ")";
                    cursor.addRow(temp);
                    queryVehicles.add(vehicleWithFavorite);
                }
            }
        }
        searchView.setSuggestionsAdapter(new SearchVehicleAdapter(this, cursor, queryVehicles));
    }

    private void executeDownloadTask() {
        refreshLayout.setRefreshing(false);
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.VISIBLE);

        double lat = 39.473;
        double lon = -6.371;
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location userLocation = null;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (userLocation == null) {
                    userLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException e) {
                Log.e("VehiclesListActivity", "Location permission denied", e);
            }
        }
        if (userLocation != null) {
            lat = userLocation.getLatitude();
            lon = userLocation.getLongitude();
        }

        vehiclesViewModel.syncVehicles(lat, lon);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mProgressBar.setVisibility(View.GONE);
            setDBLastUpdateText();
        }, 3000);
    }

    private void setNearbyVehicles() {
        if (vehicles == null) {
            return;
        }
        final double radius = 0.01;
        nearbyVehicles = new ArrayList<>();
        LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location userLocation = null;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (userLocation == null) {
                    userLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException ignored) {}
        }
        if (userLocation != null) {
            final Location finalUserLocation = userLocation;
            for (VehicleWithFavorite vehicleWithFavorite : vehicles) {
                Vehicle vehicle = vehicleWithFavorite.getVehicle();
                if ((vehicle.getLatitude() > userLocation.getLatitude() - radius)
                        && (vehicle.getLatitude() < userLocation.getLatitude() + radius)
                        && (vehicle.getLongitude() > userLocation.getLongitude() - radius)
                        && (vehicle.getLongitude() < userLocation.getLongitude() + radius)) {
                    nearbyVehicles.add(vehicleWithFavorite);
                }
            }
            nearbyVehicles.sort((v1, v2) -> {
                Vehicle vehicle1 = v1.getVehicle();
                Vehicle vehicle2 = v2.getVehicle();
                float[] result1 = new float[3];
                Location.distanceBetween(finalUserLocation.getLatitude(), finalUserLocation.getLongitude(),
                        vehicle1.getLatitude(), vehicle1.getLongitude(), result1);
                Float distance1 = result1[0];

                float[] result2 = new float[3];
                Location.distanceBetween(finalUserLocation.getLatitude(), finalUserLocation.getLongitude(),
                        vehicle2.getLatitude(), vehicle2.getLongitude(), result2);
                Float distance2 = result2[0];

                return distance1.compareTo(distance2);
            });
            getPagerAdapter().updateNearbyVehiclesFragment(nearbyVehicles);
            int locationMinutes = (int) ((System.currentTimeMillis() - userLocation.getTime()) / 60000);
            if (!nearbyVehicles.isEmpty() && locationMinutes > 10) {
                Toast.makeText(getApplicationContext(),
                        getApplicationContext().getResources().getString(R.string.location_outdated,
                                locationMinutes), Toast.LENGTH_SHORT).show();
            }
        } else {
            if (nearbyVehiclesFragment != null) {
                nearbyVehiclesFragment.setEmptyView(R.string.location_not_found);
            }
        }
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

        if (id == R.id.nav_map) {
            startActivity(new Intent(this, MapActivity.class));
            finish();
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

    private TabsPagerAdapter getPagerAdapter() {
        if (tabsPagerAdapter == null) {
            tabsPagerAdapter = new TabsPagerAdapter(this);
        }
        return tabsPagerAdapter;
    }

    private class TabsPagerAdapter extends FragmentStateAdapter {
        private static final int NUM_ITEMS = 3;

        public TabsPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);

            allVehiclesFragment = VehiclesListFragment.newInstance(VehiclesListFragment.FRAGMENT_ALL,
                    getResources().getString(R.string.no_vehicles));
            favoriteVehiclesFragment = VehiclesListFragment.newInstance(VehiclesListFragment.FRAGMENT_FAVORITES,
                    getResources().getString(R.string.no_favorite_vehicles));
            nearbyVehiclesFragment = VehiclesListFragment.newInstance(VehiclesListFragment.FRAGMENT_NEARBY,
                    getResources().getString(R.string.no_nearby_vehicles));
        }

        @Override
        public int getItemCount() {
            return NUM_ITEMS;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> nearbyVehiclesFragment;
                case 1 -> favoriteVehiclesFragment;
                case 2 -> allVehiclesFragment;
                default -> throw new IllegalArgumentException("Invalid position " + position);
            };
        }

        public CharSequence getPageTitle(int position) {
            return switch (position) {
                case 0 -> getString(R.string.nearby_vehicles);
                case 1 -> getString(R.string.favorite_vehicles);
                case 2 -> getString(R.string.all_vehicles);
                default -> null;
            };
        }

        public void updateAllVehiclesListFragment(ArrayList<VehicleWithFavorite> vehicles) {
            if (allVehiclesFragment != null) {
                allVehiclesFragment.updateVehiclesList(vehicles);
            }
        }

        public void updateFavoriteVehiclesFragment(ArrayList<VehicleWithFavorite> vehicles) {
            if (favoriteVehiclesFragment != null) {
                favoriteVehiclesFragment.updateVehiclesList(vehicles);
            }
        }

        public void updateNearbyVehiclesFragment(ArrayList<VehicleWithFavorite> vehicles) {
            if (nearbyVehiclesFragment != null) {
                nearbyVehiclesFragment.updateVehiclesList(vehicles);
            }
        }
    }
}
