package de.christianscheb.partyprojector.app.preferences;

import android.content.SharedPreferences;
import de.christianscheb.partyprojector.app.SettingsActivity;

public class AppPreferences {

    SharedPreferences preferences;

    public AppPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public String getServerBaseUrl() {
        String baseUrl = preferences.getString(SettingsActivity.KEY_PREF_SERVER, null);
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        return baseUrl;
    }
}
