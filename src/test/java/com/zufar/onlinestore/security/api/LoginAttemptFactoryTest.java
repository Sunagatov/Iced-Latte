package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptFactoryTest {
    private String userEmail = Instancio.of(String.class)
            .create();

    @Test
    @DisplayName("test creating LoginAttemptEntity")
    public void testLoginAttemptEntity(){
        LoginAttemptEntity loginAttemptEntity =
                LoginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail);

        assertTrue(loginAttemptEntity!=null);
        assertEquals(null, loginAttemptEntity.getId());
        assertEquals(userEmail, loginAttemptEntity.getUserEmail());
        assertEquals(0, loginAttemptEntity.getAttempts());
        assertEquals(null, loginAttemptEntity.getExpirationDatetime());
        assertEquals(false, loginAttemptEntity.getIsUserLocked());
        assertTrue(loginAttemptEntity.getLastModified()!=null);
    }
}