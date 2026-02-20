package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.test.config.IntegrationTestBase;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationResponse;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserRegistrationService Integration Tests")
class UserRegistrationServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should successfully register new user with valid data")
    void shouldRegisterNewUserSuccessfully() {
        final UserRegistrationRequest request = new UserRegistrationRequest(
                "John", "Doe", "john.doe@example.com", "Password123!"
        );

        final UserRegistrationResponse response = userRegistrationService.register(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());

        final UserEntity savedUser = userRepository.findByEmail("john.doe@example.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals("John", savedUser.getFirstName());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals("john.doe@example.com", savedUser.getEmail());
        assertTrue(savedUser.isAccountNonExpired());
        assertTrue(savedUser.isAccountNonLocked());
        assertTrue(savedUser.isCredentialsNonExpired());
        assertTrue(savedUser.isEnabled());
        assertFalse(savedUser.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when registering user with existing email")
    void shouldThrowExceptionForDuplicateEmail() {
        final UserRegistrationRequest firstRequest = new UserRegistrationRequest(
                "John", "Doe", "duplicate@example.com", "Password123!"
        );
        userRegistrationService.register(firstRequest);

        final UserRegistrationRequest duplicateRequest = new UserRegistrationRequest(
                "Jane", "Smith", "duplicate@example.com", "Password456!"
        );

        assertThrows(UserRegistrationException.class, () -> 
                userRegistrationService.register(duplicateRequest));
    }

    @Test
    @DisplayName("Should return false for existing email availability check")
    void shouldReturnFalseForExistingEmail() {
        final UserRegistrationRequest request = new UserRegistrationRequest(
                "John", "Doe", "existing@example.com", "Password123!"
        );
        userRegistrationService.register(request);

        assertFalse(userRegistrationService.isEmailAvailable("existing@example.com"));
    }

    @Test
    @DisplayName("Should return true for non-existing email availability check")
    void shouldReturnTrueForNonExistingEmail() {
        assertTrue(userRegistrationService.isEmailAvailable("nonexisting@example.com"));
    }
}