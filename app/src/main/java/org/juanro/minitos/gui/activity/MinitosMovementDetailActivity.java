package org.juanro.minitos.gui.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

import org.juanro.minitos.R;

public class MinitosMovementDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minitos_movement_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left + 72, systemBars.top + 72, systemBars.right + 72, systemBars.bottom + 72);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        String type = getIntent().getStringExtra("type");
        String date = getIntent().getStringExtra("date");
        String desc = getIntent().getStringExtra("desc");
        double amount = getIntent().getDoubleExtra("amount", 0.0);
        String amountStr = getIntent().getStringExtra("amountStr");

        TextView tvType = findViewById(R.id.tvMovementType);
        TextView tvAmount = findViewById(R.id.tvMovementAmount);
        TextView tvDate = findViewById(R.id.tvMovementDate);
        TextView tvDesc = findViewById(R.id.tvMovementDesc);
        ImageView ivIcon = findViewById(R.id.ivMovementIcon);

        tvDate.setText(date);
        tvDesc.setText(desc);
        tvAmount.setText(amountStr != null ? amountStr : String.format(Locale.US, "%.2f €", amount));

        if ("REGALODAR".equalsIgnoreCase(type)) {
            tvType.setText(R.string.movement_gift_sent);
            ivIcon.setImageResource(android.R.drawable.ic_menu_send);
        } else if ("REGALORECIBIR".equalsIgnoreCase(type)) {
            tvType.setText(R.string.movement_gift_received);
            ivIcon.setImageResource(android.R.drawable.ic_input_get);
        } else {
            tvType.setText(R.string.movement_recharge);
            ivIcon.setImageResource(android.R.drawable.ic_menu_add);
        }

        findViewById(R.id.btnMovementClose).setOnClickListener(v -> finish());
    }
}
