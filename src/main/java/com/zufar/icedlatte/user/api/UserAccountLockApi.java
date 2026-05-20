package com.zufar.icedlatte.user.api;

public interface UserAccountLockApi {

    int setAccountLockedStatus(String email, boolean accountNonLocked);

    void unlockExpiredLockedAccounts();
}
