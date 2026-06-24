package org.juanro.minitos.gui.activity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;
import org.juanro.minitos.data.api.config.NetworkConstants;

public class MinitosWalletActivity extends AppCompatActivity {
    private static final String TAG = "MinitosWalletActivity";

    private TextView tvBalance;
    private LinearLayout llPacksContainer;
    private EditText etPromoCode, etGiftEmail, etGiftAmount;
    
    private MinitsApiClient minitsClient;
    private MinitsAuthenticator minitsAuth;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minitos_wallet);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        View root = findViewById(R.id.wallet_root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBarLayout.setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        tvBalance = findViewById(R.id.tvWalletBalance);
        llPacksContainer = findViewById(R.id.llPacksContainer);
        etPromoCode = findViewById(R.id.etPromoCode);
        etGiftEmail = findViewById(R.id.etGiftEmail);
        etGiftAmount = findViewById(R.id.etGiftAmount);
        RecyclerView rvHistory = findViewById(R.id.rvWalletHistory);
        
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(new ArrayList<>());
        rvHistory.setAdapter(historyAdapter);

        minitsClient = new MinitsApiClient(this);
        minitsAuth = new MinitsAuthenticator(this, minitsClient);
        minitsAuth.loadSavedAuth();

        findViewById(R.id.btnApplyPromo).setOnClickListener(v -> validatePromo());
        findViewById(R.id.btnSendGift).setOnClickListener(v -> handleSendGift());

        loadData();
    }

    private void loadData() {
        CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Loading wallet data for: " + minitsAuth.getEmail());
                
                // 1. Balance
                MinitsModels.AccountStatusResponse balanceResp = minitsClient.post(NetworkConstants.PATH_ESTADO_CUENTA, new HashMap<>(), MinitsModels.AccountStatusResponse.class);
                if (balanceResp != null && balanceResp.data != null) {
                    Log.d(TAG, "Balance received: " + balanceResp.data.TOTAL);
                    runOnUiThread(() -> tvBalance.setText(String.format(java.util.Locale.US, "%.2f €", balanceResp.data.TOTAL)));
                }

                // 2. Packs
                MinitsModels.DatosSQLRequest packsReq = new MinitsModels.DatosSQLRequest("paquetesMinutosWhere", Arrays.asList(minitsAuth.getEmail(), "SP"));
                MinitsModels.MinutePacksResponse packsResp = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, packsReq, MinitsModels.MinutePacksResponse.class);
                if (packsResp != null && packsResp.data != null) {
                    Log.d(TAG, "Packs received: " + packsResp.data.length);
                    runOnUiThread(() -> displayPacks(packsResp.data));
                }

                // 3. History (Current Month)
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                String filter1 = String.format(java.util.Locale.US, "C.USUARIO='%s' AND YEAR(c.FECHA)=%d AND MONTH(c.FECHA)=%d", minitsAuth.getEmail(), year, month);
                String filter2 = String.format(java.util.Locale.US, "C.USUARIO='%s' AND YEAR(c.FECHA_INI_SYS)=%d AND MONTH(c.FECHA_INI_SYS)=%d", minitsAuth.getEmail(), year, month);
                
                MinitsModels.DatosSQLRequest historyReq = new MinitsModels.DatosSQLRequest("MonederoCliente", Arrays.asList(filter1, filter2, "SP"));
                MinitsModels.WalletResponse historyResp = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, historyReq, MinitsModels.WalletResponse.class);
                if (historyResp != null && historyResp.data != null) {
                    runOnUiThread(() -> historyAdapter.updateItems(Arrays.asList(historyResp.data)));
                }

            } catch (org.juanro.minitos.data.api.MinitsAuthException e) {
                handleAuthError();
            } catch (Exception e) {
                Log.e(TAG, "Error loading wallet data", e);
            }
        });
    }

    private void handleAuthError() {
        runOnUiThread(() -> {
            minitsAuth.logout();
            Toast.makeText(this, "Su sesión ha expirado. Por favor, identifíquese de nuevo.", Toast.LENGTH_LONG).show();
            startActivity(new android.content.Intent(this, MinitosProfileActivity.class));
            finish();
        });
    }

    private void displayPacks(MinitsModels.MinutePack[] packs) {
        llPacksContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (MinitsModels.MinutePack pack : packs) {
            View view = inflater.inflate(R.layout.item_minute_pack, llPacksContainer, false);
            ((TextView) view.findViewById(R.id.tvPackName)).setText(pack.PAQUETE);
            ((TextView) view.findViewById(R.id.tvPackReceive)).setText(pack.MAP_TOTALTL2);
            ((TextView) view.findViewById(R.id.tvPackPay)).setText(getString(R.string.wallet_pack_pay, pack.MAP_TOTALTL));
            
            view.setOnClickListener(v -> buyPack(pack));
            llPacksContainer.addView(view);
        }
    }

    private void validatePromo() {
        String code = etPromoCode.getText().toString();
        if (code.isEmpty()) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                MinitsModels.DatosSQLRequest req = new MinitsModels.DatosSQLRequest("paquetesMinutosDescuentoWhere", Arrays.asList(minitsAuth.getEmail(), "SP", code));
                MinitsModels.GenericResponse<?> resp = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, req, MinitsModels.GenericResponse.class);
                runOnUiThread(() -> {
                    if (resp.hasError) Toast.makeText(this, "Código no válido", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this, "Código aceptado", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error validating promo", e);
            }
        });
    }

    private void handleSendGift() {
        String toEmail = etGiftEmail.getText().toString();
        String amountStr = etGiftAmount.getText().toString();

        if (toEmail.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Complete el email e importe", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            Toast.makeText(this, "Importe inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Regalar Saldo")
                .setMessage("¿Quieres enviar " + String.format(java.util.Locale.US, "%.2f €", amount) + " a " + toEmail + "?")
                .setPositiveButton("Enviar", (dialog, which) -> executeGift(toEmail, amount))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executeGift(String toEmail, double amount) {
        CompletableFuture.runAsync(() -> {
            try {
                MinitsModels.RegalarSaldoRequest req = new MinitsModels.RegalarSaldoRequest(minitsAuth.getEmail(), toEmail, amount);
                MinitsModels.GenericResponse<?> resp = minitsClient.post(NetworkConstants.PATH_REGALAR_SALDO, req, MinitsModels.GenericResponse.class);

                runOnUiThread(() -> {
                    if (!resp.hasError) {
                        Toast.makeText(this, "¡Saldo enviado con éxito!", Toast.LENGTH_LONG).show();
                        etGiftEmail.setText("");
                        etGiftAmount.setText("");
                        loadData(); // Refresh balance and history
                    } else {
                        String msg = "Error al enviar saldo";
                        if ("R2".equals(resp.message)) msg = "No puedes enviarte saldo a ti mismo";
                        else if (resp.data != null) msg = resp.data.toString();
                        
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Gift error", e);
            }
        });
    }

    private void buyPack(MinitsModels.MinutePack pack) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmar compra")
            .setMessage("¿Quieres comprar el " + pack.PAQUETE + " por " + pack.MAP_TOTALTL + "?")
            .setPositiveButton("Pagar", (dialog, which) -> executePurchase(pack))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void executePurchase(MinitsModels.MinutePack pack) {
        CompletableFuture.runAsync(() -> {
            try {
                MinitsModels.ComprarPaqueteRequest req = new MinitsModels.ComprarPaqueteRequest(minitsAuth.getEmail(), pack.CODIGO, etPromoCode.getText().toString());
                MinitsModels.GenericResponse<?> resp = minitsClient.post(NetworkConstants.PATH_COMPRAR_PAQUETE, req, MinitsModels.GenericResponse.class);
                
                runOnUiThread(() -> {
                    if (!resp.hasError) {
                        new AlertDialog.Builder(this)
                            .setMessage("Paquete comprado con éxito. El saldo ha sido cargado en tu cuenta.")
                            .setPositiveButton("Aceptar", null)
                            .show();
                        loadData(); // Refresh balance and history
                    } else {
                        Toast.makeText(this, "Error: " + resp.data, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Purchase error", e);
            }
        });
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<MinitsModels.WalletMovement> items;

        public HistoryAdapter(List<MinitsModels.WalletMovement> items) {
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
            holder.desc.setText(item.MAP_COCHE != null ? item.MAP_COCHE : item.TIPO);
            holder.date.setText(item.MAP_FECHAHORA);
            holder.amount.setText(item.MAP_IMPORTEGRID);
            
            if (item.MAP_KM != null && !item.MAP_KM.isEmpty() && !"0 km".equalsIgnoreCase(item.MAP_KM)) {
                holder.distance.setText(item.MAP_KM);
                holder.distance.setVisibility(View.VISIBLE);
            } else {
                holder.distance.setVisibility(View.GONE);
            }

            if (item.MAP_COLORDINERO != null) {
                try {
                    holder.amount.setTextColor(Color.parseColor(item.MAP_COLORDINERO));
                } catch (Exception e) { holder.amount.setTextColor(Color.BLACK); }
            }

            if ("GASTO".equals(item.TIPO)) {
                holder.itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(MinitosWalletActivity.this, MinitosTripSummaryActivity.class);
                    intent.putExtra("vehicle", item.MAP_COCHE);
                    intent.putExtra("dateRange", item.MAP_FECHAHORA);
                    intent.putExtra("totalCost", item.IMPORTE);
                    intent.putExtra("initialCost", 0.0); // Not in history usually
                    intent.putExtra("standbyCost", 0.0); // Parse from string if needed
                    intent.putExtra("drivingCost", item.IMPORTE);
                    intent.putExtra("batteryStart", "--%");
                    intent.putExtra("batteryEnd", "--%");
                    intent.putExtra("distance", item.MAP_KM);
                    intent.putExtra("reservationId", item.NUMERORESERVA);
                    intent.putExtra("year", item.MAP_YEAR);
                    intent.putExtra("month", item.MAP_MES);
                    intent.putExtra("idReserva", item.ID);
                    startActivity(intent);
                });
            } else if (item.TIPO != null) {
                holder.itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(MinitosWalletActivity.this, MinitosMovementDetailActivity.class);
                    intent.putExtra("type", item.TIPO);
                    intent.putExtra("date", item.MAP_FECHAHORA);
                    intent.putExtra("desc", item.MAP_COCHE); // This field contains description for gifts
                    intent.putExtra("amount", item.IMPORTE);
                    intent.putExtra("amountStr", item.MAP_IMPORTEGRID);
                    startActivity(intent);
                });
            }
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
