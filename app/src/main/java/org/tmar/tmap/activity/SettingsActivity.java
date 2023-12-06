package org.tmar.tmap.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.tmar.tmap.R;
import org.tmar.tmap.view.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }
}
