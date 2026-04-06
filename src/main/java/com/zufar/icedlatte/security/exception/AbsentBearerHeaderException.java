package com.zufar.icedlatte.security.exception;

public class AbsentBearerHeaderException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Bearer authentication header is absent";

    public AbsentBearerHeaderException() {
        super(DEFAULT_MESSAGE);
    }

    public AbsentBearerHeaderException(String message) {
        super(message);
    }

    public AbsentBearerHeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
