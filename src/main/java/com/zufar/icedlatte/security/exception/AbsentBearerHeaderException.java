package com.zufar.icedlatte.security.exception;

import org.springframework.security.core.AuthenticationException;

public class AbsentBearerHeaderException extends AuthenticationException {

    public AbsentBearerHeaderException() {
        super("Bearer authentication header is absent");
    }
}
