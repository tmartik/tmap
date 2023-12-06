package org.tmar.tmap.view;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import org.tmar.tmap.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
