package de.christianscheb.partyprojector.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import de.christianscheb.partyprojector.app.httpclient.WebApiClient;
import de.christianscheb.partyprojector.app.httpclient.WebApiClientException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PHOTO = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final String PREF_CURRENT_PHOTO_PATH = "currentPhotoPath";
    private EditText editText;
    private Button sendMessageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.messageTextField);
        sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSendMessage(View view) {
        String message = getMessageText();
        if (message.length() == 0) {
            return; // Nothing to do
        }

        Log.d(getLocalClassName(), "Send message: " + message);
        setMessageInputState(false);
        new PostMessageTask().execute(message);
    }

    private String getMessageText() {
        return editText.getText().toString();
    }

    private void resetMessage() {
        editText.setText(null);
    }

    private void setMessageInputState(boolean enabled) {
        editText.setEnabled(enabled);
        sendMessageButton.setEnabled(enabled);
    }

    public void onSelectPicture(View view) {
        Log.d(getLocalClassName(), "Select picture");
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    public void onCapturePicture(View view) {
        Log.d(getLocalClassName(), "Capture picture");
        File imageFile;
        try {
            imageFile = createTemporaryImageFile();
        } catch (IOException e) {
            e.printStackTrace();
            showToast(getString(R.string.writeFailed));
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createTemporaryImageFile() throws IOException {
        Log.d(getLocalClassName(), "Generate file path for capture");
        String imageFileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File storageDir = new File(pictureDir.getAbsolutePath(), "PartyProjector");
        if (!storageDir.isDirectory()) {
            if (!storageDir.mkdir()) {
                throw new IOException("Could not create directory " + storageDir.getAbsolutePath());
            }
        }

        File image = new File(storageDir.getAbsolutePath(), imageFileName + ".jpg");
        Log.d(getLocalClassName(), "Target file for capture: " + image.getAbsolutePath());
        setCurrentPhotoPath(image.getAbsolutePath());

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case SELECT_PHOTO:
                onPictureSelected(resultCode, data);
                break;
            case REQUEST_IMAGE_CAPTURE:
                onPictureCaptured(resultCode, data);
                break;
        }
    }

    private void onPictureCaptured(int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            String currentPhotoPath = getCurrentPhotoPath();
            Log.d(getLocalClassName(), "Picture captured");
            if (currentPhotoPath != null) {
                Log.d(getLocalClassName(), "Captured picture file is " + currentPhotoPath);
                Uri picture = Uri.fromFile(new File(currentPhotoPath));
                galleryAddPic(picture);
                sendPicture(picture);
            }
        }
    }

    private void setCurrentPhotoPath(String path) {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(PREF_CURRENT_PHOTO_PATH, path);
        editor.apply();
    }

    private String getCurrentPhotoPath() {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        return settings.getString(PREF_CURRENT_PHOTO_PATH, null);
    }

    private void galleryAddPic(Uri picture) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(picture);
        this.sendBroadcast(mediaScanIntent);
    }

    private void onPictureSelected(int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            Uri selectedImage = data.getData();
            Log.d(getLocalClassName(), "Selected picture is " + selectedImage);
            sendPicture(selectedImage);
        }
    }

    private void sendPicture(Uri picture) {
        try {
            InputStream stream = getContentResolver().openInputStream(picture);
            new PostPictureTask().execute(stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                int duration = text.length() > 50 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    private String getServerBaseUrl() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String baseUrl = preferences.getString(SettingsActivity.KEY_PREF_SERVER, null);
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        return baseUrl;
    }

    private class PostMessageTask extends AsyncTask<String, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(String... messages) {
            try {
                WebApiClient client = new WebApiClient(getServerBaseUrl());
                client.sendMessage(messages[0]);
                return true;
            } catch (WebApiClientException e) {
                e.printStackTrace();
                showToast(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            resetMessage();
            setMessageInputState(true);
            if (isSuccess) {
                showToast(getString(R.string.message_sent));
            }
        }
    }

    private class PostPictureTask extends AsyncTask<InputStream, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(InputStream... pictures) {
            try {
                WebApiClient client = new WebApiClient(getServerBaseUrl());
                client.sendPicture(pictures[0]);
                return true;
            } catch (WebApiClientException e) {
                e.printStackTrace();
                showToast(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            if (isSuccess) {
                showToast(getString(R.string.picture_sent));
            }
        }
    }
}


