package com.zufar.icedlatte.security.exception.session;

import com.zufar.icedlatte.security.exception.AuthException;

import java.util.UUID;

public final class SessionOwnershipException extends AuthException {

    public SessionOwnershipException(UUID sessionId) {
        super("Session does not belong to the requesting user: " + sessionId);
    }
}
