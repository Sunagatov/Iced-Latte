package com.zufar.onlinestore.security.signin.attempts.exception;

import com.zufar.onlinestore.security.signin.UserAccountLockedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class SignInExceptionHandler {

    @ExceptionHandler(UserAccountLockedException.class)
    public ResponseEntity<String> handleUserAccountLockedException(UserAccountLockedException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
    }

}

