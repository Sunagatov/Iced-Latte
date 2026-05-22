package com.zufar.icedlatte.security.signin.exception;


import java.util.UUID;

public final class SessionOwnershipException extends AuthSecurityException {

    public SessionOwnershipException(UUID sessionId) {
        super("Session does not belong to the requesting user: " + sessionId);
    }
}
