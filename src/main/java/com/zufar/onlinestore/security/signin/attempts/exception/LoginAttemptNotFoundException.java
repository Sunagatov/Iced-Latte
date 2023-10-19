package com.zufar.onlinestore.security.signin.attempts.exception;

public class LoginAttemptNotFoundException extends RuntimeException {

	public LoginAttemptNotFoundException(String userEmail) {
		super(String.format("LoginAttempt for user with the email = '%s' is not found", userEmail));
	}

}
