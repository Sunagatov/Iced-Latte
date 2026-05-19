package com.zufar.icedlatte.common.audit;

import java.util.UUID;

/**
 * Marker interface for principals that carry a UUID identity.
 * Implemented by UserEntity to allow AuditConfig to extract the user ID
 * without depending on the user module.
 */
public interface Identifiable {
    UUID getId();
}
