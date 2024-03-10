package com.zufar.icedlatte.ai.reviewvalidator.exception;

public class AiServiceConnectionException extends RuntimeException {

    public AiServiceConnectionException(final String aiServiceUrl) {
        super(String.format("Failed to open httpURLConnection with url = '%s'", aiServiceUrl));
    }

    public AiServiceConnectionException() {
        super(String.format("Failed to open httpURLConnection"));
    }
}

