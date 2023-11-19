package com.zufar.icedlatte.security.exception;

public class AccountLockedException extends RuntimeException {

	public AccountLockedException() {
		super("Your account is locked.");
	}
}
