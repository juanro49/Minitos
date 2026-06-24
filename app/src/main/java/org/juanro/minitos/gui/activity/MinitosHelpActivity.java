package org.juanro.minitos.gui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;
import org.juanro.minitos.data.api.config.NetworkConstants;

import com.google.android.material.appbar.AppBarLayout;

public class MinitosHelpActivity extends AppCompatActivity {
    private static final String TAG = "MinitosHelp";
    private HelpAdapter adapter;
    private MinitsApiClient minitsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minitos_help);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.help_root_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBarLayout.setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        RecyclerView rv = findViewById(R.id.rvHelp);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HelpAdapter();
        rv.setAdapter(adapter);

        minitsClient = new MinitsApiClient(this);
        MinitsAuthenticator auth = new MinitsAuthenticator(this, minitsClient);
        auth.loadSavedAuth();
        minitsClient.setAuth(auth.getToken(), auth.getEmail());

        fetchHelpItems();
    }

    private void fetchHelpItems() {
        CompletableFuture.runAsync(() -> {
            try {
                MinitsModels.HelpResponse response = minitsClient.post(NetworkConstants.PATH_AYUDA, new java.util.HashMap<>(), MinitsModels.HelpResponse.class);
                
                List<MinitsModels.HelpItem> items = new ArrayList<>();
                
                // Add FAQ as first item
                MinitsModels.HelpItem faq = new MinitsModels.HelpItem();
                faq.TITULO = "Preguntas Frecuentes (FAQ)";
                faq.DESCRIPCION = "Resolución de dudas sobre el servicio, tarifas y zonas.";
                faq.DOCUMENTO = NetworkConstants.URL_FAQ;
                faq.TIPO = "WEB";
                items.add(faq);

                if (response != null && response.data != null) {
                    items.addAll(Arrays.asList(response.data));
                }

                runOnUiThread(() -> adapter.setItems(items));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching help", e);
                runOnUiThread(() -> Toast.makeText(this, "Error cargando ayuda", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.ViewHolder> {
        private List<MinitsModels.HelpItem> items = new ArrayList<>();

        public void setItems(List<MinitsModels.HelpItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_help, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MinitsModels.HelpItem item = items.get(position);
            holder.title.setText(item.TITULO);
            holder.desc.setText(item.DESCRIPCION);
            
            if ("YOUTUBE".equalsIgnoreCase(item.TIPO)) {
                holder.playIcon.setVisibility(View.VISIBLE);
                holder.icon.setImageResource(android.R.drawable.presence_video_online);
            } else {
                holder.playIcon.setVisibility(View.GONE);
                holder.icon.setImageResource(android.R.drawable.ic_menu_help);
            }

            holder.itemView.setOnClickListener(v -> {
                if (item.DOCUMENTO != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.DOCUMENTO));
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, desc;
            ImageView icon, playIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvHelpTitle);
                desc = itemView.findViewById(R.id.tvHelpDesc);
                icon = itemView.findViewById(R.id.ivHelpIcon);
                playIcon = itemView.findViewById(R.id.ivPlayIcon);
            }
        }
    }
}
