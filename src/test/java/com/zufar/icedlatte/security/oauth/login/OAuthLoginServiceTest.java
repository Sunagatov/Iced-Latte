package com.zufar.icedlatte.security.oauth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;
import com.zufar.icedlatte.security.oauth.entity.OAuthIdentityEntity;
import com.zufar.icedlatte.security.oauth.repository.OAuthIdentityRepository;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserRegistrationApi;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    private static final String AUTH_CODE = "auth-code";
    private static final String GOOGLE_SUBJECT = "google-subject";
    private static final String EXISTING_EMAIL = "existing@example.com";
    private static final String NEW_EMAIL = "new@example.com";
    private static final AuthSessionRequestMetadata REQUEST_METADATA =
            new AuthSessionRequestMetadata("TestAgent", "127.0.0.1");

    @Mock
    private OAuthProviderClient providerClient;

    @Mock
    private OAuthIdentityRepository oAuthIdentityRepository;

    @Mock
    private UserAuthenticationApi userAuthenticationApi;

    @Mock
    private UserRegistrationApi userRegistrationApi;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionTokenService sessionTokenService;

    private OAuthLoginService service;

    @BeforeEach
    void setUp() {
        service = new OAuthLoginService(
                List.of(providerClient),
                oAuthIdentityRepository,
                userAuthenticationApi,
                userRegistrationApi,
                passwordEncoder,
                sessionTokenService);
    }

    @Test
    void reusesExistingOauthIdentityAndReturnsTokens() {
        UserAuthenticationSnapshot existingUser = activeUser();
        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, true, "Alice", "Existing");
        stubIdentity(existingUser);
        stubToken();
        AuthenticationTokens response = handle();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(sessionTokenService).issueForNewSession(any(), eq(REQUEST_METADATA));
    }

    @Test
    void linksNewOauthIdentityToExistingUserWhenProviderEmailIsVerified() {
        UserAuthenticationSnapshot existingUser = activeUser();
        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, true, "Alice", "Existing");
        stubNoIdentity();
        stubUser(existingUser);
        stubToken();
        AuthenticationTokens response = handle();
        assertThat(response.accessToken()).isEqualTo("access-token");
        ArgumentCaptor<OAuthIdentityEntity> savedIdentities = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(oAuthIdentityRepository).save(savedIdentities.capture());
        OAuthIdentityEntity savedIdentity = savedIdentities.getValue();
        assertThat(savedIdentity.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(savedIdentity.getProviderSubject()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(savedIdentity.getEmail()).isEqualTo(EXISTING_EMAIL);
        assertThat(savedIdentity.getUserId()).isEqualTo(existingUser.userId());
    }

    @Test
    void normalizesProviderSubjectAndEmailBeforeLookupAndIdentitySave() {
        UserAuthenticationSnapshot existingUser = activeUser();
        stubProfile("  google-subject  ", " Existing@Example.com ", true, "Alice", "Existing");
        stubNoIdentity();
        stubUser(existingUser);
        stubToken();
        handle();
        ArgumentCaptor<OAuthIdentityEntity> savedIdentities = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(oAuthIdentityRepository).save(savedIdentities.capture());
        OAuthIdentityEntity savedIdentity = savedIdentities.getValue();
        assertThat(savedIdentity.getProviderSubject()).isEqualTo("google-subject");
        assertThat(savedIdentity.getEmail()).isEqualTo("existing@example.com");
    }

    @Test
    void createsNewOauthUserWhenEmailDoesNotExist() {
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, true, "New", "User");
        stubNoIdentity();
        stubNoUser();
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");
        UserAuthenticationSnapshot newUser = activeUser(UUID.randomUUID(), NEW_EMAIL, "encoded-random-password");
        when(userRegistrationApi.registerOAuthUser(any(), any(), any(), any())).thenReturn(newUser);
        when(sessionTokenService.issueForNewSession(any(), eq(REQUEST_METADATA)))
                .thenReturn(tokenPair());
        AuthenticationTokens response = handle();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRegistrationApi)
                .registerOAuthUser(eq("New"), eq("User"), eq(NEW_EMAIL), eq("encoded-random-password"));
        verify(sessionTokenService).issueForNewSession(any(), eq(REQUEST_METADATA));
        ArgumentCaptor<OAuthIdentityEntity> savedIdentities = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(oAuthIdentityRepository).save(savedIdentities.capture());
        assertThat(savedIdentities.getValue().getProviderSubject()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(savedIdentities.getValue().getUserId()).isEqualTo(newUser.userId());
    }

    @Test
    void recoversWhenConcurrentCallbackCreatesOauthIdentityFirst() {
        UserAuthenticationSnapshot existingUser = activeUser(UUID.randomUUID(), NEW_EMAIL, "secret");
        OAuthIdentityEntity existingIdentity = OAuthIdentityEntity.builder()
                .provider(OAuthProvider.GOOGLE)
                .providerSubject(GOOGLE_SUBJECT)
                .email(NEW_EMAIL)
                .userId(existingUser.userId())
                .build();
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, true, "New", "User");
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, GOOGLE_SUBJECT))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingIdentity));
        when(userAuthenticationApi.findUserAuthenticationByEmail(NEW_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");
        when(userRegistrationApi.registerOAuthUser(any(), any(), any(), any())).thenReturn(existingUser);
        when(oAuthIdentityRepository.save(any(OAuthIdentityEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate identity"));
        when(userAuthenticationApi.findUserAuthenticationById(existingUser.userId()))
                .thenReturn(Optional.of(existingUser));
        stubToken();

        AuthenticationTokens response = handle();

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userAuthenticationApi).findUserAuthenticationById(existingUser.userId());
    }

    @Test
    void rejectsCreatingNewUserWhenProviderEmailIsUnverified() {
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, false, "New", "User");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account email is not verified.");

        verifyNoInteractions(oAuthIdentityRepository, userAuthenticationApi, userRegistrationApi, sessionTokenService);
    }

    @Test
    void usesSafeFallbackNamesWhenProviderProfileHasNoFirstOrLastName() {
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, true, " ", null);
        stubNoIdentity();
        stubNoUser();
        stubNewUserSave();
        handle();
        verify(userRegistrationApi)
                .registerOAuthUser(eq("OAuth"), eq("User"), eq(NEW_EMAIL), eq("encoded-random-password"));
    }

    @Test
    void trimsAndCapsProviderNamesBeforeCreatingLocalUser() {
        String longName = "x".repeat(140);
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, true, "  Ada  ", longName);
        stubNoIdentity();
        stubNoUser();
        stubNewUserSave();
        handle();
        ArgumentCaptor<String> firstName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> lastName = ArgumentCaptor.forClass(String.class);
        verify(userRegistrationApi)
                .registerOAuthUser(
                        firstName.capture(), lastName.capture(), eq(NEW_EMAIL), eq("encoded-random-password"));
        assertThat(firstName.getValue()).isEqualTo("Ada");
        assertThat(lastName.getValue()).hasSize(128);
    }

    @Test
    void rejectsProviderAccountsWithTooLongSubject() {
        stubProfile("s".repeat(256), "email@example.com", true, "Long", "Subject");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account subject is too long.");

        verifyNoInteractions(oAuthIdentityRepository, userAuthenticationApi, userRegistrationApi, sessionTokenService);
    }

    @Test
    void rejectsProviderAccountsWithTooLongEmail() {
        stubProfile(GOOGLE_SUBJECT, "a".repeat(245) + "@example.com", true, "Long", "Email");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account email is too long.");

        verifyNoInteractions(oAuthIdentityRepository, userAuthenticationApi, userRegistrationApi, sessionTokenService);
    }

    @Test
    void rejectsLinkingWhenUnverifiedProviderEmailExistsLocally() {
        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, false, "New", "User");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account email is not verified.");

        verifyNoInteractions(oAuthIdentityRepository, userAuthenticationApi, userRegistrationApi, sessionTokenService);
    }

    @Test
    void rejectsExistingOauthIdentityWhenLocalUserCannotSignIn() {
        UserAuthenticationSnapshot lockedUser = new UserAuthenticationSnapshot(
                UUID.randomUUID(), EXISTING_EMAIL, "secret", List.of("USER"), true, false, true, true);

        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, true, "Alice", "Existing");
        stubIdentity(lockedUser);

        assertThatThrownBy(this::handle)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("OAuth account is not available.");
    }

    @Test
    void rejectsEmailLinkedOauthLoginWhenLocalUserCannotSignIn() {
        UserAuthenticationSnapshot disabledUser = new UserAuthenticationSnapshot(
                UUID.randomUUID(), EXISTING_EMAIL, "secret", List.of("USER"), true, true, true, false);

        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, true, "Alice", "Existing");
        stubNoIdentity();
        stubUser(disabledUser);

        assertThatThrownBy(this::handle)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("OAuth account is not available.");
    }

    @Test
    void rejectsProviderAccountsWithoutSubject() {
        stubProfile(" ", "email@example.com", true, "No", "Subject");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account has no subject.");

        verifyNoInteractions(oAuthIdentityRepository, userAuthenticationApi, userRegistrationApi, sessionTokenService);
    }

    @Test
    void rejectsProviderAccountsWithoutAnEmailAddress() {
        stubProfile(GOOGLE_SUBJECT, " ", true, "No", "Email");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account has no email.");

        verifyNoInteractions(oAuthIdentityRepository, userAuthenticationApi, userRegistrationApi, sessionTokenService);
    }

    @Test
    void returnsEmptyWhenProviderClientIsNotRegistered() {
        OAuthLoginService service = new OAuthLoginService(
                List.of(),
                oAuthIdentityRepository,
                userAuthenticationApi,
                userRegistrationApi,
                passwordEncoder,
                sessionTokenService);

        assertThat(service.findClient(OAuthProvider.GOOGLE)).isEmpty();
    }

    private AuthenticationTokens handle() {
        return service.handle(OAuthProvider.GOOGLE, AUTH_CODE, REQUEST_METADATA);
    }

    private void stubProfile(
            String providerSubject, String email, boolean emailVerified, String firstName, String lastName) {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode(AUTH_CODE))
                .thenReturn(profile(providerSubject, email, emailVerified, firstName, lastName));
    }

    private void stubNoIdentity() {
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(
                        OAuthProvider.GOOGLE, OAuthLoginServiceTest.GOOGLE_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void stubIdentity(UserAuthenticationSnapshot user) {
        OAuthIdentityEntity identity = OAuthIdentityEntity.builder()
                .provider(OAuthProvider.GOOGLE)
                .providerSubject(OAuthLoginServiceTest.GOOGLE_SUBJECT)
                .email(OAuthLoginServiceTest.EXISTING_EMAIL)
                .userId(user.userId())
                .build();
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(
                        OAuthProvider.GOOGLE, OAuthLoginServiceTest.GOOGLE_SUBJECT))
                .thenReturn(Optional.of(identity));
        when(userAuthenticationApi.findUserAuthenticationById(user.userId())).thenReturn(Optional.of(user));
    }

    private void stubUser(UserAuthenticationSnapshot user) {
        when(userAuthenticationApi.findUserAuthenticationByEmail(OAuthLoginServiceTest.EXISTING_EMAIL))
                .thenReturn(Optional.of(user));
    }

    private void stubNoUser() {
        when(userAuthenticationApi.findUserAuthenticationByEmail(OAuthLoginServiceTest.NEW_EMAIL))
                .thenReturn(Optional.empty());
    }

    private void stubToken() {
        when(sessionTokenService.issueForNewSession(any(), eq(REQUEST_METADATA)))
                .thenReturn(tokenPair());
    }

    private void stubNewUserSave() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");
        when(userRegistrationApi.registerOAuthUser(any(), any(), any(), any()))
                .thenReturn(activeUser(UUID.randomUUID(), NEW_EMAIL, "encoded-random-password"));
        when(sessionTokenService.issueForNewSession(any(), eq(REQUEST_METADATA)))
                .thenReturn(tokenPair());
    }

    private static AuthenticationTokens tokenPair() {
        return new AuthenticationTokens("access-token", "refresh-token");
    }

    private static UserAuthenticationSnapshot activeUser() {
        return activeUser(UUID.randomUUID(), OAuthLoginServiceTest.EXISTING_EMAIL, "secret");
    }

    private static UserAuthenticationSnapshot activeUser(UUID userId, String email, String password) {
        return new UserAuthenticationSnapshot(userId, email, password, List.of("USER"), true, true, true, true);
    }

    private static OAuthProfile profile(
            String providerSubject, String email, boolean emailVerified, String firstName, String lastName) {
        return new OAuthProfile(providerSubject, email, emailVerified, firstName, lastName);
    }
}
