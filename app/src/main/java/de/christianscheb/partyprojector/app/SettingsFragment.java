package de.christianscheb.partyprojector.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import de.christianscheb.partyprojector.app.httpclient.WebApiClient;
import de.christianscheb.partyprojector.app.httpclient.WebApiClientException;
import de.christianscheb.partyprojector.app.preferences.AppPreferences;
import de.christianscheb.partyprojector.app.preferences.TestConnectionPreference;

import java.util.List;

public class SettingsFragment extends PreferenceFragment {

    public static final String PREF_CAMERA = "pref_camera";
    public static final String PREF_SIZE = "pref_size";
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

        // Camera preference
        final ListPreference cameraPreference = (ListPreference) findPreference(PREF_CAMERA);
        setCameraPreferences(cameraPreference);
        cameraPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setCameraPreferences(cameraPreference);
                return false;
            }
        });

        // JPEG size preference
        final ListPreference sizePreference = (ListPreference) findPreference(PREF_SIZE);
        setSizePreferences(sizePreference, cameraPreference);
        sizePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setSizePreferences(sizePreference, cameraPreference);
                return false;
            }
        });
    }

    private void setCameraPreferences(final ListPreference cameraPreference) {
        final int numberOfCameras = Camera.getNumberOfCameras();
        final CharSequence[] entries = new CharSequence[numberOfCameras];
        final CharSequence[] entryValues = new CharSequence[numberOfCameras];
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < numberOfCameras; cameraIndex++) {
            Camera.getCameraInfo(cameraIndex, cameraInfo);
            String cameraFacing;
            switch (cameraInfo.facing) {
                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    cameraFacing = getString(R.string.camera_location_back);
                    break;
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    cameraFacing = getString(R.string.camera_location_front);
                    break;
                default:
                    cameraFacing = getString(R.string.camera_location_unknown);
            }

            entries[cameraIndex] = cameraFacing;
            entryValues[cameraIndex] = String.valueOf(cameraIndex);
        }

        cameraPreference.setEntries(entries);
        cameraPreference.setEntryValues(entryValues);
    }

    private void setSizePreferences(final ListPreference sizePreference, final ListPreference cameraPreference) {
        final String cameraPreferenceValue = cameraPreference.getValue();
        final int cameraIndex;
        if (cameraPreferenceValue != null) {
            cameraIndex = Integer.parseInt(cameraPreferenceValue);
        } else {
            cameraIndex = 0;
        }
        final Camera camera = Camera.open(cameraIndex);
        final Camera.Parameters params = camera.getParameters();
        camera.release();

        final List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
        CharSequence[] entries = new CharSequence[supportedPreviewSizes.size()];
        CharSequence[] entryValues = new CharSequence[supportedPreviewSizes.size()];
        for (int previewSizeIndex = 0; previewSizeIndex < supportedPreviewSizes.size(); previewSizeIndex++) {
            Camera.Size supportedPreviewSize = supportedPreviewSizes.get(previewSizeIndex);
            entries[previewSizeIndex] = supportedPreviewSize.width + "x" + supportedPreviewSize.height;
            entryValues[previewSizeIndex] = String.valueOf(previewSizeIndex);
        }

        sizePreference.setEntries(entries);
        sizePreference.setEntryValues(entryValues);
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
