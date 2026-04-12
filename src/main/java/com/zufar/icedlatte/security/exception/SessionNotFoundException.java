package com.zufar.icedlatte.security.exception;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
