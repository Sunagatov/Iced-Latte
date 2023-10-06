package com.zufar.onlinestore.security.exception;

public class AccountLockedException extends RuntimeException {

	public AccountLockedException(String cause) {
		super(cause);
	}
}
