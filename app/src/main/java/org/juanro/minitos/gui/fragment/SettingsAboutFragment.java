package org.juanro.minitos.gui.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.juanro.minitos.BuildConfig;
import org.juanro.minitos.R;

public class SettingsAboutFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pref_about, rootKey);

        Preference versionPref = findPreference("pref_version");
        if (versionPref != null) {
            versionPref.setSummary(BuildConfig.VERSION_NAME + (BuildConfig.DEBUG ? "-debug" : ""));
        }
    }
}
