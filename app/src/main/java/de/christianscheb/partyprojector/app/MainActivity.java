package de.christianscheb.partyprojector.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);
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

    public void sendMessage(View view) {
        String message = getMessage();
        if (message.length() == 0) {
            return; // Nothing to do
        }

        setMessageInputState(false);
        Log.i(this.getLocalClassName(), "Send message: " + message);
        new PostMessageTask().execute(message);
    }

    private String getMessage() {
        EditText editText = (EditText) findViewById(R.id.messageTextField);
        return editText.getText().toString();
    }

    private void resetMessage() {
        EditText editText = (EditText) findViewById(R.id.messageTextField);
        editText.setText(null);
    }

    private void setMessageInputState(boolean enabled) {
        EditText editText = (EditText) findViewById(R.id.messageTextField);
        editText.setEnabled(enabled);
        Button button = (Button) findViewById(R.id.sendMessageButton);
        button.setEnabled(enabled);
    }

    private String getServerBaseUrl() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String baseUrl = preferences.getString(SettingsActivity.KEY_PREF_SERVER, null);
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        return baseUrl;
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

    private class PostMessageTask extends AsyncTask<String, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(String... messages) {
            try {
                WebApiClient client = new WebApiClient(getServerBaseUrl());
                client.sendMessage(messages[0]);
                return true;
            } catch (WebApiClientException e) {
                e.getStackTrace();
                showToast(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            resetMessage();
            setMessageInputState(true);
            if (isSuccess) {
                showToast(getString(R.string.messageSent));
            }
        }
    }
}


