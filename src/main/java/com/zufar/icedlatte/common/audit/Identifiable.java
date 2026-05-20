package com.zufar.icedlatte.common.audit;

import java.util.UUID;

/**
 * Marker interface for principals that carry a UUID identity.
 * Allows infrastructure code to extract a user ID without depending on
 * a concrete user module entity.
 */
public interface Identifiable {
    UUID getId();
}
