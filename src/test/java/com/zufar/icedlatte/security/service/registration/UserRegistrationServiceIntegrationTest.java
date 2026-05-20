package com.zufar.icedlatte.security.service.registration;

import com.zufar.icedlatte.test.config.IntegrationTestBase;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserRegistrationService Integration Tests")
class UserRegistrationServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserRepository userRepository;

    private static final MockHttpServletRequest MOCK_REQUEST = new MockHttpServletRequest();

    @Test
    @DisplayName("Should successfully register new user with valid data")
    void shouldRegisterNewUserSuccessfully() {
        final UserRegistrationRequest request = new UserRegistrationRequest(
                "John", "Doe", "john.doe@example.com", "Password123!"
        );

        final UserAuthenticationResponse response = userRegistrationService.register(request, MOCK_REQUEST);
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
        userRegistrationService.register(firstRequest, MOCK_REQUEST);

        final UserRegistrationRequest duplicateRequest = new UserRegistrationRequest(
                "Jane", "Smith", "duplicate@example.com", "Password456!"
        );

        assertThrows(UserRegistrationException.class, () ->
                userRegistrationService.register(duplicateRequest, MOCK_REQUEST));
    }

    @Test
    @DisplayName("Should return false for existing email availability check")
    void shouldReturnFalseForExistingEmail() {
        final UserRegistrationRequest request = new UserRegistrationRequest(
                "John", "Doe", "existing@example.com", "Password123!"
        );
        userRegistrationService.register(request, MOCK_REQUEST);

        assertTrue(userRepository.findByEmail("existing@example.com").isPresent());
    }

    @Test
    @DisplayName("Should return true for non-existing email availability check")
    void shouldReturnTrueForNonExistingEmail() {
        assertTrue(userRepository.findByEmail("nonexisting@example.com").isEmpty());
    }
}