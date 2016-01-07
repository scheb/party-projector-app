package de.christianscheb.partyprojector.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
        EditText editText = (EditText) findViewById(R.id.messageTextField);
        String message = editText.getText().toString();
        if (message.length() == 0) {
            return; // Nothing to do
        }

        new PostMessageTask().execute(message);
    }

    private String getServerBaseUrl() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String baseUrl = preferences.getString(SettingsActivity.KEY_PREF_SERVER, null);
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        return baseUrl;
    }

    private void showToast(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private class PostMessageTask extends AsyncTask<String, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(String... messages) {
            try {
                WebApiClient client = new WebApiClient(getServerBaseUrl());
                client.sendMessage(messages[0]);
                return true;
            } catch (WebApiClientException e) {
                showToast(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                showToast("Done!");
            }
        }
    }
}


