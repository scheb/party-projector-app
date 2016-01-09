package de.christianscheb.partyprojector.app;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import de.christianscheb.partyprojector.app.preferences.AppPreferences;

public class SettingsActivity extends PreferenceActivity {

    public static final String KEY_PREF_SERVER = "pref_server";
    private AppPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsFragment fragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }
}
