package com.zufar.icedlatte.security.exception;

import java.util.UUID;

public final class SessionNotFoundException extends AuthException {

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
