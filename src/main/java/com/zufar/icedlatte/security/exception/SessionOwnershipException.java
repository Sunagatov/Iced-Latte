package com.zufar.icedlatte.security.exception;

import java.util.UUID;

public class SessionOwnershipException extends RuntimeException {

    public SessionOwnershipException(UUID sessionId) {
        super("Session does not belong to the requesting user: " + sessionId);
    }
}
