package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoginAttemptFactoryTest {
    private String userEmail = "TestEmail";

    @Test
    @DisplayName("Test LoginAttemptEntity creation")
    void shouldCreateInitialFailedLoggedAttemptEntityWithDefaultValues() {
        LoginAttemptEntity loginAttemptEntity = LoginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail);

        assertNotNull(loginAttemptEntity);
        assertNull(loginAttemptEntity.getId());
        assertEquals(userEmail, loginAttemptEntity.getUserEmail());
        assertEquals(0, loginAttemptEntity.getAttempts());
        assertNull(loginAttemptEntity.getExpirationDatetime());
        assertFalse(loginAttemptEntity.getIsUserLocked());
        assertNotNull(loginAttemptEntity.getLastModified());
    }
}