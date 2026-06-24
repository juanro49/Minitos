package org.juanro.minitos;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.juanro.minitos.model.MinitosDatabase;
import org.juanro.minitos.util.Preferences;

public class MinitosApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MinitosApplication";
    private static MinitosApplication instance;
    private final List<Activity> activities = Collections.synchronizedList(new ArrayList<>());

    private static synchronized void setInstance(MinitosApplication application) {
        instance = application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setInstance(this);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                activities.add(activity);
            }
            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            @Override
            public void onActivityResumed(@NonNull Activity activity) {}
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                activities.remove(activity);
            }
        });

        DynamicColors.applyToActivitiesIfAvailable(
                this,
                new DynamicColorsOptions.Builder()
                        .setPrecondition((activity, themeResId) -> new Preferences(activity).isDynamicColorEnabled())
                        .build()
        );

        applyThemeConfiguration();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("ui_theme".equals(key) || "ui_dynamic_color".equals(key) || "pref_map_layer".equals(key)) {
            Log.d(TAG, "Preference changed: " + key + ". Refreshing activities...");
            if ("ui_theme".equals(key) || "ui_dynamic_color".equals(key)) {
                applyThemeConfiguration();
            }

            synchronized (activities) {
                for (Activity activity : new ArrayList<>(activities)) {
                    activity.recreate();
                }
            }
        }
    }

    private void applyThemeConfiguration() {
        Preferences preferences = new Preferences(this);
        String theme = preferences.getTheme();
        int mode = switch (theme) {
            case "light" -> AppCompatDelegate.MODE_NIGHT_NO;
            case "dark" -> AppCompatDelegate.MODE_NIGHT_YES;
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };

        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    public static MinitosApplication getInstance() {
        return instance;
    }

    public static void closeDatabases() {
        if (instance != null) {
            MinitosDatabase.resetInstance();
        }
    }
}
