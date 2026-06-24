package org.juanro.minitos.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class Preferences {
    public static final String KEY_THEME = "ui_theme";
    public static final String KEY_DYNAMIC_COLOR = "ui_dynamic_color";
    public static final String KEY_DB_LAST_UPDATE = "db_last_update";

    private final SharedPreferences prefs;

    public Preferences(@NonNull Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, "system");
    }

    public boolean isDynamicColorEnabled() {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, true);
    }

    public long getDbLastUpdate() {
        return prefs.getLong(KEY_DB_LAST_UPDATE, -1);
    }

    public void setDbLastUpdate(long timestamp) {
        prefs.edit().putLong(KEY_DB_LAST_UPDATE, timestamp).apply();
    }
}
