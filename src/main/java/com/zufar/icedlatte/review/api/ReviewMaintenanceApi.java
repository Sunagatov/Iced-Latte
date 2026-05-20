package com.zufar.icedlatte.review.api;

/**
 * Narrow contract for maintenance/migration operations on reviews.
 * Used by astartup to refresh denormalized counts during bootstrap.
 */
public interface ReviewMaintenanceApi {

    void refreshAllCounts();
}
