package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("LoginAttemptFactory Tests")
class LoginAttemptFactoryTest {

    private final LoginAttemptFactory loginAttemptFactory = new LoginAttemptFactory();

    @Test
    @DisplayName("Should Create Initial Failed Login Attempt Entity with Default Values")
    void shouldCreateInitialFailedLoggedAttemptEntityWithDefaultValues() {
        String userEmail = "TestEmail";
        LoginAttemptEntity loginAttemptEntity = loginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail);

        assertNotNull(loginAttemptEntity, "LoginAttemptEntity should not be null");
        assertNull(loginAttemptEntity.getId(), "Id should be null for a new entity");
        assertEquals(userEmail, loginAttemptEntity.getUserEmail(), "User email should match");
        assertEquals(0, loginAttemptEntity.getAttempts(), "Attempts should be initialized to 0");
        assertNull(loginAttemptEntity.getExpirationDatetime(), "Expiration datetime should be null initially");
        assertFalse(loginAttemptEntity.getIsUserLocked(), "User should not be locked initially");
        assertNotNull(loginAttemptEntity.getLastModified(), "Last modified date should be set");
    }
}
