package com.orbyfied.minem.exception;

public class ClientReadException extends RuntimeException {

    public ClientReadException() {
    }

    public ClientReadException(String message) {
        super(message);
    }

    public ClientReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientReadException(Throwable cause) {
        super(cause);
    }

    public ClientReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
