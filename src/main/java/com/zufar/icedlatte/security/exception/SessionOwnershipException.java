package com.zufar.icedlatte.security.exception;

import java.util.UUID;

public final class SessionOwnershipException extends AuthException {

    public SessionOwnershipException(UUID sessionId) {
        super("Session does not belong to the requesting user: " + sessionId);
    }
}
