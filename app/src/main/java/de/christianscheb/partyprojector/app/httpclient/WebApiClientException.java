package de.christianscheb.partyprojector.app.httpclient;

public class WebApiClientException extends Exception {

    public WebApiClientException(String detailMessage) {
        super(detailMessage);
    }

    public WebApiClientException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
