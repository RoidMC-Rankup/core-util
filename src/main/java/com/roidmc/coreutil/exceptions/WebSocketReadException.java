package com.roidmc.coreutil.exceptions;

public class WebSocketReadException extends Exception{

    public WebSocketReadException() {
    }

    public WebSocketReadException(String message) {
        super(message);
    }

    public WebSocketReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketReadException(Throwable cause) {
        super(cause);
    }

    public WebSocketReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
