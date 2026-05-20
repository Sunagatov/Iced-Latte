package com.zufar.icedlatte.security.exception.session;

import com.zufar.icedlatte.security.exception.AuthException;

import java.util.UUID;

public final class SessionNotFoundException extends AuthException {

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
