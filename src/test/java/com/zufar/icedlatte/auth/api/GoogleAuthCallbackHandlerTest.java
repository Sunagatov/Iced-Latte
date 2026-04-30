package com.zufar.icedlatte.auth.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.api.SessionTokenService;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleAuthCallbackHandler unit tests")
class GoogleAuthCallbackHandlerTest {

    @Mock private GoogleTokenExchanger googleTokenExchanger;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SessionTokenService sessionTokenService;
    @Mock private HttpServletRequest request;

    @InjectMocks private GoogleAuthCallbackHandler handler;

    @Test
    @DisplayName("reuses an existing user and returns access and refresh tokens")
    void reusesExistingUserAndReturnsTokens() throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = payload("existing@example.com", "Alice", "Existing");
        UserEntity existingUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .password("secret")
                .build();

        when(googleTokenExchanger.exchange("auth-code")).thenReturn(payload);
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));
        when(sessionTokenService.issueForNewSession(existingUser, request))
                .thenReturn(tokenPair());

        UserAuthenticationResponse response = handler.handle("auth-code", request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(userRepository).findByEmail("existing@example.com");
        verify(sessionTokenService).issueForNewSession(existingUser, request);
    }

    @Test
    @DisplayName("creates a new OAuth user when the email does not exist")
    void createsNewOauthUserWhenEmailDoesNotExist() throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = payload("new@example.com", "New", "User");
        when(googleTokenExchanger.exchange("auth-code")).thenReturn(payload);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");

        UUID userId = UUID.randomUUID();
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(userId);
            }
            return user;
        });
        when(sessionTokenService.issueForNewSession(any(UserEntity.class), eq(request)))
                .thenReturn(tokenPair());

        UserAuthenticationResponse response = handler.handle("auth-code", request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<UserEntity> savedUsers = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(savedUsers.capture());
        UserEntity savedUser = savedUsers.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("New");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-random-password");
        assertThat(savedUser.isOauthUser()).isTrue();
        assertThat(savedUser.isAccountNonExpired()).isTrue();
        assertThat(savedUser.isAccountNonLocked()).isTrue();
        assertThat(savedUser.isCredentialsNonExpired()).isTrue();
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getAuthorities())
                .singleElement()
                .extracting(UserGrantedAuthority::getAuthority)
                .isEqualTo(Authority.USER.name());
        assertThat(savedUser.getAuthorities())
                .singleElement()
                .extracting(UserGrantedAuthority::getUser)
                .isSameAs(savedUser);
        verify(sessionTokenService).issueForNewSession(savedUser, request);
    }

    @Test
    @DisplayName("rejects Google accounts without an email address")
    void rejectsGoogleAccountsWithoutAnEmailAddress() throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = payload(" ", "No", "Email");
        when(googleTokenExchanger.exchange("auth-code")).thenReturn(payload);

        assertThatThrownBy(() -> handler.handle("auth-code", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Google account has no email");

        verifyNoInteractions(userRepository, sessionTokenService);
    }

    private static GoogleIdToken.Payload payload(String email, String firstName, String lastName) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        payload.put("given_name", firstName);
        payload.put("family_name", lastName);
        return payload;
    }

    private static UserAuthenticationResponse tokenPair() {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken("access-token");
        response.setRefreshToken("refresh-token");
        return response;
    }
}
