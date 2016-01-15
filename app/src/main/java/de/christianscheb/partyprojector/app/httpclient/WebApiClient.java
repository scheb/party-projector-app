package de.christianscheb.partyprojector.app.httpclient;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.zip.GZIPOutputStream;

public class WebApiClient {

    private String baseUrl;
    private final String CRLF = "\r\n";
    private final String CHARSET = "UTF-8";

    public WebApiClient(String baseUrl) throws WebApiClientException {
        this.baseUrl = baseUrl;
        if (baseUrl == null) {
            throw new WebApiClientException("You need to configure a server URL");
        }
    }

    public boolean testConnection() throws WebApiClientException {
        URL urlObj = getUrl("");
        Log.d(getClass().getSimpleName(), "Request to: " + urlObj);

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);

            //Get Response
            JSONObject response = new JSONObject(getResponse(urlConnection));
            boolean isSuccess = response.getBoolean("success");
            String serverName = response.getString("server");
            Log.d(getClass().getSimpleName(), "Request sent");
            return isSuccess && serverName != null;
        } catch (ProtocolException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } catch (IOException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } catch (JSONException e) {
            throw new WebApiClientException("Malformed response", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public void sendMessage(String message) throws WebApiClientException {
        URL urlObj = getUrl("message");
        Log.d(getClass().getSimpleName(), "Request to: " + urlObj);

        String urlParameters;
        try {
            urlParameters = "message=" + URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new WebApiClientException("Could not encode message", e);
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setConnectTimeout(1000);
            urlConnection.setConnectTimeout(3000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            //Send request
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());
            request.writeBytes(urlParameters);
            request.flush();
            request.close();

            //Get Response
            JSONObject response = new JSONObject(getResponse(urlConnection));
            boolean isSuccess = response.getBoolean("success");
            Log.d(getClass().getSimpleName(), "Request sent");
            if (!isSuccess) {
                throw new WebApiClientException("Request failed");
            }
        } catch (ProtocolException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } catch (IOException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } catch (JSONException e) {
            throw new WebApiClientException("Malformed response", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public void sendPicture(InputStream file) throws WebApiClientException {
        URL urlObj = getUrl("picture");
        Log.d(getClass().getSimpleName(), "Send picture " + file + " to " + urlObj);

        HttpURLConnection connection = null;
        try {
            String boundary = Long.toHexString(System.currentTimeMillis());
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setConnectTimeout(1000);
            connection.setConnectTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, CHARSET), true);

            // Send text file
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"picture\"; filename=\"picture.jpg\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + CHARSET).append(CRLF);
            writer.append("Content-Encoding: gzip").append(CRLF);
            writer.append(CRLF).flush();
            copyStream(file, output);
            file.close();
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush();
            writer.append("--" + boundary + "--").append(CRLF).flush();

            //Get Response
            JSONObject response = new JSONObject(getResponse(connection));
            boolean isSuccess = response.getBoolean("success");
            Log.d(getClass().getSimpleName(), "Request sent");
            if (!isSuccess) {
                throw new WebApiClientException("Request failed");
            }
        } catch (ProtocolException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } catch (IOException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } catch (JSONException e) {
            throw new WebApiClientException("Malformed response", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        GZIPOutputStream gzipOut = new GZIPOutputStream(output);
        while ((bytesRead = input.read(buffer)) != -1) {
            gzipOut.write(buffer, 0, bytesRead);
        }
        gzipOut.flush();
    }

    private URL getUrl(String endpoint) throws WebApiClientException {
        String url = baseUrl + endpoint;
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new WebApiClientException("Could not connect to " + url, e);
        }
    }

    private String getResponse(URLConnection urlConnection) throws IOException {
        InputStream is = urlConnection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rd.close();

        return response.toString();
    }
}
