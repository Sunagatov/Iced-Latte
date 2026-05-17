package com.zufar.icedlatte.auth.api;

import com.zufar.icedlatte.auth.entity.OAuthIdentityEntity;
import com.zufar.icedlatte.auth.repository.OAuthIdentityRepository;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.api.SessionTokenService;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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
@DisplayName("OAuthLoginService unit tests")
class OAuthLoginServiceTest {

    @Mock private OAuthProviderClient providerClient;
    @Mock private OAuthIdentityRepository oAuthIdentityRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SessionTokenService sessionTokenService;
    @Mock private HttpServletRequest request;

    private OAuthLoginService service;

    @BeforeEach
    void setUp() {
        service = new OAuthLoginService(
                List.of(providerClient),
                oAuthIdentityRepository,
                userRepository,
                passwordEncoder,
                sessionTokenService
        );
    }

    @Test
    @DisplayName("reuses an existing OAuth identity and returns access and refresh tokens")
    void reusesExistingOauthIdentityAndReturnsTokens() {
        UserEntity existingUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .password("secret")
                .build();
        OAuthIdentityEntity identity = OAuthIdentityEntity.builder()
                .provider(OAuthProvider.GOOGLE)
                .providerSubject("google-subject")
                .email("existing@example.com")
                .user(existingUser)
                .build();

        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile("google-subject", "existing@example.com", true, "Alice", "Existing"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject"))
                .thenReturn(Optional.of(identity));
        when(sessionTokenService.issueForNewSession(existingUser, request)).thenReturn(tokenPair());

        UserAuthenticationResponse response = service.handle(OAuthProvider.GOOGLE, "auth-code", request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(sessionTokenService).issueForNewSession(existingUser, request);
    }

    @Test
    @DisplayName("links a new OAuth identity to an existing user when provider email is verified")
    void linksNewOauthIdentityToExistingUserWhenProviderEmailIsVerified() {
        UserEntity existingUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .password("secret")
                .build();

        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile("google-subject", "existing@example.com", true, "Alice", "Existing"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));
        when(sessionTokenService.issueForNewSession(existingUser, request)).thenReturn(tokenPair());

        UserAuthenticationResponse response = service.handle(OAuthProvider.GOOGLE, "auth-code", request);

        assertThat(response.getToken()).isEqualTo("access-token");
        ArgumentCaptor<OAuthIdentityEntity> savedIdentities = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(oAuthIdentityRepository).save(savedIdentities.capture());
        OAuthIdentityEntity savedIdentity = savedIdentities.getValue();
        assertThat(savedIdentity.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(savedIdentity.getProviderSubject()).isEqualTo("google-subject");
        assertThat(savedIdentity.getEmail()).isEqualTo("existing@example.com");
        assertThat(savedIdentity.getUser()).isSameAs(existingUser);
    }

    @Test
    @DisplayName("creates a new OAuth user when the email does not exist")
    void createsNewOauthUserWhenEmailDoesNotExist() {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile("google-subject", "new@example.com", true, "New", "User"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject"))
                .thenReturn(Optional.empty());
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
        when(sessionTokenService.issueForNewSession(any(UserEntity.class), eq(request))).thenReturn(tokenPair());

        UserAuthenticationResponse response = service.handle(OAuthProvider.GOOGLE, "auth-code", request);

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

        ArgumentCaptor<OAuthIdentityEntity> savedIdentities = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(oAuthIdentityRepository).save(savedIdentities.capture());
        assertThat(savedIdentities.getValue().getProviderSubject()).isEqualTo("google-subject");
        assertThat(savedIdentities.getValue().getUser()).isSameAs(savedUser);
    }

    @Test
    @DisplayName("creates a new user when provider email is not verified and no local user has that email")
    void createsNewUserWhenUnverifiedProviderEmailDoesNotExistLocally() {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile("google-subject", "new@example.com", false, "New", "User"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTokenService.issueForNewSession(any(UserEntity.class), eq(request))).thenReturn(tokenPair());

        service.handle(OAuthProvider.GOOGLE, "auth-code", request);

        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("uses safe fallback names when provider profile has no first or last name")
    void usesSafeFallbackNamesWhenProviderProfileHasNoFirstOrLastName() {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile("google-subject", "new@example.com", true, " ", null));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTokenService.issueForNewSession(any(UserEntity.class), eq(request))).thenReturn(tokenPair());

        service.handle(OAuthProvider.GOOGLE, "auth-code", request);

        ArgumentCaptor<UserEntity> savedUsers = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(savedUsers.capture());
        assertThat(savedUsers.getValue().getFirstName()).isEqualTo("OAuth");
        assertThat(savedUsers.getValue().getLastName()).isEqualTo("User");
    }

    @Test
    @DisplayName("rejects linking when provider email is not verified and local user has that email")
    void rejectsLinkingWhenUnverifiedProviderEmailExistsLocally() {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile("google-subject", "existing@example.com", false, "New", "User"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(UserEntity.builder().email("existing@example.com").build()));

        assertThatThrownBy(() -> service.handle(OAuthProvider.GOOGLE, "auth-code", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account email is not verified.");
    }

    @Test
    @DisplayName("rejects provider accounts without a subject")
    void rejectsProviderAccountsWithoutSubject() {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile(" ", "email@example.com", true, "No", "Subject"));

        assertThatThrownBy(() -> service.handle(OAuthProvider.GOOGLE, "auth-code", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account has no subject.");

        verifyNoInteractions(oAuthIdentityRepository, userRepository, sessionTokenService);
    }

    @Test
    @DisplayName("rejects provider accounts without an email address")
    void rejectsProviderAccountsWithoutAnEmailAddress() {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode("auth-code"))
                .thenReturn(profile("google-subject", " ", true, "No", "Email"));

        assertThatThrownBy(() -> service.handle(OAuthProvider.GOOGLE, "auth-code", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account has no email.");

        verifyNoInteractions(oAuthIdentityRepository, userRepository, sessionTokenService);
    }

    @Test
    @DisplayName("returns empty when a provider client is not registered")
    void returnsEmptyWhenProviderClientIsNotRegistered() {
        OAuthLoginService service = new OAuthLoginService(
                List.of(),
                oAuthIdentityRepository,
                userRepository,
                passwordEncoder,
                sessionTokenService
        );

        assertThat(service.findClient(OAuthProvider.GOOGLE)).isEmpty();
    }

    private static UserAuthenticationResponse tokenPair() {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken("access-token");
        response.setRefreshToken("refresh-token");
        return response;
    }

    private static OAuthProfile profile(String providerSubject,
                                        String email,
                                        boolean emailVerified,
                                        String firstName,
                                        String lastName) {
        return new OAuthProfile(providerSubject, email, emailVerified, firstName, lastName);
    }
}
