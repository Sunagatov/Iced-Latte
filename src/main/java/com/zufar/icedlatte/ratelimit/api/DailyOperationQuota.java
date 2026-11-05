package com.zufar.icedlatte.ratelimit.api;

public interface DailyOperationQuota {
    /** Reserves one attempt before an external operation. Failed attempts are not refunded. */
    void reserve(String scope, long dailyLimit);
}
