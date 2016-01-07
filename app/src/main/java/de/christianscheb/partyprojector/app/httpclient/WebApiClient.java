package de.christianscheb.partyprojector.app.httpclient;

import java.io.*;
import java.net.*;

public class WebApiClient {

    private String baseUrl;

    public WebApiClient(String baseUrl) throws WebApiClientException {
        this.baseUrl = baseUrl;
        if (baseUrl == null) {
            throw new WebApiClientException("You need to configure a server URL");
        }
    }

    public void sendMessage(String message) throws WebApiClientException {
        String url = baseUrl + "message";
        URL urlObj;
        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            throw new WebApiClientException("Could not connect to " + url, e);
        }

        String urlParameters;
        try {
            urlParameters = "message=" + URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new WebApiClientException("Could not encode message", e);
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            //Get Response
            InputStream is = urlConnection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String response = getResponse(rd);
        } catch (ProtocolException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } catch (IOException e) {
            throw new WebApiClientException("Could not execute HTTP request", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private String getResponse(BufferedReader rd) throws IOException {
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rd.close();
        return response.toString();
    }
}
