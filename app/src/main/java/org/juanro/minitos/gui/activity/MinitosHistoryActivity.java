package org.juanro.minitos.gui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;

import com.google.android.material.appbar.AppBarLayout;

public class MinitosHistoryActivity extends AppCompatActivity {
    private static final String TAG = "MinitosHistoryActivity";

    private RecyclerView rvTrips;
    private ProgressBar progressBar;
    private TextView tvNoTrips;
    private AppBarLayout appBarLayout;
    
    private MinitsApiClient minitsClient;
    private MinitsAuthenticator minitsAuth;
    private TripsAdapter tripsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minitos_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        appBarLayout = findViewById(R.id.app_bar);
        rvTrips = findViewById(R.id.rvTripsHistory);
        progressBar = findViewById(R.id.progressBar);
        tvNoTrips = findViewById(R.id.tvNoTrips);
        
        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        tripsAdapter = new TripsAdapter(new ArrayList<>());
        rvTrips.setAdapter(tripsAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.trips_root_layout), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBarLayout.setPadding(0, systemBars.top, 0, 0);
            rvTrips.setPadding(rvTrips.getPaddingLeft(), rvTrips.getPaddingTop(), 
                             rvTrips.getPaddingRight(), systemBars.bottom);
            return windowInsets;
        });

        minitsClient = new MinitsApiClient(this);
        minitsAuth = new MinitsAuthenticator(this, minitsClient);
        minitsAuth.loadSavedAuth();

        loadTrips();
    }

    private void loadTrips() {
        progressBar.setVisibility(View.VISIBLE);
        CompletableFuture.runAsync(() -> {
            try {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                
                String filter1 = String.format(java.util.Locale.US, "C.USUARIO='%s' AND YEAR(c.FECHA)=%d AND MONTH(c.FECHA)=%d", minitsAuth.getEmail(), year, month);
                String filter2 = String.format(java.util.Locale.US, "C.USUARIO='%s' AND YEAR(c.FECHA_INI_SYS)=%d AND MONTH(c.FECHA_INI_SYS)=%d", minitsAuth.getEmail(), year, month);
                
                MinitsModels.DatosSQLRequest historyReq = new MinitsModels.DatosSQLRequest("MonederoCliente", Arrays.asList(filter1, filter2, "SP"));
                MinitsModels.WalletResponse historyResp = minitsClient.post("/api/app/DatosSQL", historyReq, MinitsModels.WalletResponse.class);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (historyResp != null && historyResp.data != null) {
                        List<MinitsModels.WalletMovement> trips = new ArrayList<>();
                        for (MinitsModels.WalletMovement m : historyResp.data) {
                            if ("GASTO".equals(m.TIPO)) {
                                trips.add(m);
                            }
                        }
                        
                        if (trips.isEmpty()) {
                            tvNoTrips.setVisibility(View.VISIBLE);
                        } else {
                            tvNoTrips.setVisibility(View.GONE);
                            tripsAdapter.updateItems(trips);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading trips", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar viajes", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.ViewHolder> {
        private List<MinitsModels.WalletMovement> items;

        public TripsAdapter(List<MinitsModels.WalletMovement> items) {
            this.items = items;
        }

        public void updateItems(List<MinitsModels.WalletMovement> newItems) {
            this.items = newItems;
            //noinspection NotifyDataSetChanged
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet_movement, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MinitsModels.WalletMovement item = items.get(position);
            holder.desc.setText(item.MAP_COCHE != null ? item.MAP_COCHE : "Viaje en Minits");
            holder.date.setText(item.MAP_FECHAHORA);
            holder.amount.setText(item.MAP_IMPORTEGRID);
            
            if (item.MAP_KM != null && !item.MAP_KM.isEmpty() && !"0 km".equalsIgnoreCase(item.MAP_KM)) {
                holder.distance.setText(item.MAP_KM);
                holder.distance.setVisibility(View.VISIBLE);
            } else {
                holder.distance.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MinitosHistoryActivity.this, MinitosTripSummaryActivity.class);
                intent.putExtra("vehicle", item.MAP_COCHE);
                intent.putExtra("dateRange", item.MAP_FECHAHORA);
                intent.putExtra("totalCost", item.IMPORTE);
                intent.putExtra("distance", item.MAP_KM);
                intent.putExtra("reservationId", item.NUMERORESERVA);
                intent.putExtra("year", item.MAP_YEAR);
                intent.putExtra("month", item.MAP_MES);
                intent.putExtra("idReserva", item.ID);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView desc, date, amount, distance;
            public ViewHolder(View v) {
                super(v);
                desc = v.findViewById(R.id.tvMovementDesc);
                date = v.findViewById(R.id.tvMovementDate);
                amount = v.findViewById(R.id.tvMovementAmount);
                distance = v.findViewById(R.id.tvMovementDistance);
            }
        }
    }
}
