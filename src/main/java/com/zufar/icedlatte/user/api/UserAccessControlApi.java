package com.zufar.icedlatte.user.api;

import java.util.UUID;

/**
 * Cross-module API for managing user credentials and account access state.
 * Consumed by the security module for password resets and account locking.
 */
public interface UserAccessControlApi {

    void changePassword(UUID userId, String newPassword);

    int lockAccount(String email);

    int unlockAccount(String email);

    void unlockExpiredAccounts();
}
