/*
 * Based on Peepers StreamCameraActivity
 *
 * Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foxdogstudios.peepers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import de.christianscheb.partyprojector.app.R;
import de.christianscheb.partyprojector.app.httpclient.WebApiClient;
import de.christianscheb.partyprojector.app.httpclient.WebApiClientException;
import de.christianscheb.partyprojector.app.preferences.AppPreferences;

public class StreamCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String PREF_CAMERA = "pref_camera";
    private static final String PREF_FLASH_LIGHT = "pref_flash_light";
    private static final String PREF_JPEG_SIZE = "pref_size";
    private static final String PREF_JPEG_QUALITY = "pref_jpeg_quality";
    private static final int PREF_CAMERA_INDEX_DEF = 0;
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final int PREF_JPEG_QUALITY_DEF = 40;
    private static final int PREF_PREVIEW_SIZE_INDEX_DEF = -1;
    public static final int STREAM_PORT = 8089;

    private boolean mRunning = false;
    private boolean mPreviewDisplayCreated = false;
    private SurfaceHolder mPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;

    private SharedPreferences mPrefs;
    private int mCameraIndex = PREF_CAMERA_INDEX_DEF;
    private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
    private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
    private int mPreviewSizeIndex = PREF_PREVIEW_SIZE_INDEX_DEF;

    private AppPreferences preferences;
    private TextView streamStatusText;
    private ImageView streamStatusIcon;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        preferences = new AppPreferences(PreferenceManager.getDefaultSharedPreferences(this));

        mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        mPreviewDisplay.addCallback(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(StreamCameraActivity.this);
        mPreviewSizeIndex = getPrefInt(PREF_JPEG_SIZE, PREF_PREVIEW_SIZE_INDEX_DEF);
        mJpegQuality = getPrefInt(PREF_JPEG_QUALITY, PREF_JPEG_QUALITY_DEF);

        updateUi();
        tryStartCameraStreamer();

        // Animate connect icon
        streamStatusText = (TextView) findViewById(R.id.streamStatusText);
        streamStatusText.setText(getString(R.string.stream_connect));
        streamStatusIcon = (ImageView) findViewById(R.id.streamStatusIcon);
        streamStatusIcon.setImageResource(R.drawable.connect_anim);
        startAnimation(streamStatusIcon);

        new StartStreamTask().execute();
    }

    private void startAnimation(ImageView icon) {
        Drawable imageResource = icon.getDrawable();
        if (imageResource instanceof AnimationDrawable) {
            AnimationDrawable animation = (AnimationDrawable) imageResource;
            if (!animation.isRunning()) {
                animation.start();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRunning = true;
        updateUi();
        tryStartCameraStreamer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRunning = false;
        ensureCameraStreamerStopped();
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        // Ignored
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        mPreviewDisplayCreated = true;
        tryStartCameraStreamer();
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        mPreviewDisplayCreated = false;
        ensureCameraStreamerStopped();
    }

    private void tryStartCameraStreamer() {
        if (mRunning && mPreviewDisplayCreated && mPrefs != null) {
            mCameraStreamer = new CameraStreamer(mCameraIndex, mUseFlashLight, STREAM_PORT, mPreviewSizeIndex, mJpegQuality, mPreviewDisplay);
            mCameraStreamer.start();
        }
    }

    private void ensureCameraStreamerStopped() {
        if (mCameraStreamer != null) {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        }
    }

    private int getPrefInt(final String key, final int defValue) {
        // We can't just call getInt because the preference activity
        // saves everything as a string.
        try {
            return Integer.parseInt(mPrefs.getString(key, null));
        } catch (final NullPointerException e) {
            return defValue;
        } catch (final NumberFormatException e) {
            return defValue;
        }
    }

    private void updateUi() {
        mCameraIndex = getPrefInt(PREF_CAMERA, PREF_CAMERA_INDEX_DEF);
        if (hasFlashLight()) {
            if (mPrefs != null) {
                mUseFlashLight = mPrefs.getBoolean(PREF_FLASH_LIGHT, PREF_FLASH_LIGHT_DEF);
            } else {
                mUseFlashLight = PREF_FLASH_LIGHT_DEF;
            }
        } else {
            mUseFlashLight = false;
        }
    }

    private boolean hasFlashLight() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void showConnectionFailedMessage() {
        final Activity self = this;
        runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(self)
                        .setTitle(R.string.connection_failed_title)
                        .setMessage(getString(R.string.connection_failed_msg))
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

    private void setStreamingActive() {
        streamStatusIcon.setImageResource(R.drawable.stream_live);
        streamStatusText.setText(getString(R.string.stream_live));
    }

    private void setStreamingFailed() {
        streamStatusIcon.setImageResource(R.drawable.stream_disconnected);
        streamStatusText.setText(getString(R.string.stream_disconnected));
    }

    private class StartStreamTask extends AsyncTask<Void, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                WebApiClient client = new WebApiClient(preferences.getServerBaseUrl());
                return client.startStream();
            } catch (WebApiClientException e) {
                e.printStackTrace();
                showConnectionFailedMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            if (isSuccess) {
                setStreamingActive();
            } else {
                setStreamingFailed();
            }
        }
    }
}
