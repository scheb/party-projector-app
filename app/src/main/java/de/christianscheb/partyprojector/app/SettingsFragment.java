package de.christianscheb.partyprojector.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import de.christianscheb.partyprojector.app.httpclient.WebApiClient;
import de.christianscheb.partyprojector.app.httpclient.WebApiClientException;
import de.christianscheb.partyprojector.app.preferences.AppPreferences;
import de.christianscheb.partyprojector.app.preferences.TestConnectionPreference;

public class SettingsFragment extends PreferenceFragment {

    private AppPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        TestConnectionPreference testConnPreference = (TestConnectionPreference) findPreference("pref_server_test");
        testConnPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(getClass().getSimpleName(), "Test connection");
                new TestConnectionTask().execute();
                return true;
            }
        });
        preferences = new AppPreferences(PreferenceManager.getDefaultSharedPreferences(getActivity()));
    }

    private void showResult(final boolean result) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.pref_server_test)
                        .setMessage(getString(result ? R.string.connection_success : R.string.connection_failed))
                        .setNeutralButton(R.string.ok, new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
        });
    }
    private class TestConnectionTask extends AsyncTask<String, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                WebApiClient client = new WebApiClient(preferences.getServerBaseUrl());
                client.testConnection();
                return true;
            } catch (WebApiClientException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            showResult(isSuccess);
        }
    }
}
