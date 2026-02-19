package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityDefaultsProviderTest {

    private final UserEntityDefaultsProvider userEntityDefaultsProvider = new UserEntityDefaultsProvider();

    @Test
    @DisplayName("Should apply correct default values to user entity")
    void shouldApplyCorrectDefaults() {
        final UserEntity userEntity = new UserEntity();

        userEntityDefaultsProvider.applyDefaults(userEntity);

        assertTrue(userEntity.isAccountNonExpired());
        assertTrue(userEntity.isAccountNonLocked());
        assertTrue(userEntity.isCredentialsNonExpired());
        assertTrue(userEntity.isEnabled());
    }

    @Test
    @DisplayName("Should not throw exception for null user entity")
    void shouldHandleNullUserEntity() {
        assertDoesNotThrow(() -> userEntityDefaultsProvider.applyDefaults(null));
    }
}