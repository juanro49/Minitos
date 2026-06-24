package org.juanro.minitos.gui.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import org.juanro.minitos.R;
import org.juanro.minitos.gui.fragment.SettingsAboutFragment;
import org.juanro.minitos.gui.fragment.SettingsAdvancedFragment;
import org.juanro.minitos.gui.fragment.SettingsFragment;

import com.google.android.material.appbar.AppBarLayout;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBarLayout.setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        if (savedInstanceState == null) {
            Fragment targetFragment = new SettingsFragment();
            String EXTRA_SUBSCREEN_NAME = "extra_subscreen_name";
            if(getIntent().hasExtra(EXTRA_SUBSCREEN_NAME)) {
                String subscreenExtra = getIntent().getStringExtra(EXTRA_SUBSCREEN_NAME);
                if("advanced".equals(subscreenExtra)) {
                    targetFragment = new SettingsAdvancedFragment();
                    setTitle(getResources().getString(R.string.pref_title_advanced));
                } else if ("about".equals(subscreenExtra)) {
                    targetFragment = new SettingsAboutFragment();
                    setTitle(getResources().getString(R.string.pref_title_about));
                }
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, targetFragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
