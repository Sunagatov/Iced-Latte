package com.zufar.icedlatte.security.oauth.service;

import com.zufar.icedlatte.security.oauth.entity.OAuthIdentityEntity;
import com.zufar.icedlatte.security.oauth.repository.OAuthIdentityRepository;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.oauth.api.OAuthProvider;
import com.zufar.icedlatte.security.oauth.api.OAuthProviderClient;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;
import com.zufar.icedlatte.security.service.session.SessionTokenService;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    private static final String AUTH_CODE = "auth-code";
    private static final String GOOGLE_SUBJECT = "google-subject";
    private static final String EXISTING_EMAIL = "existing@example.com";
    private static final String NEW_EMAIL = "new@example.com";

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
    void reusesExistingOauthIdentityAndReturnsTokens() {
        UserEntity existingUser = activeUser();
        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, true, "Alice", "Existing");
        stubIdentity(existingUser);
        stubToken(existingUser);
        UserAuthenticationResponse response = handle();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(sessionTokenService).issueForNewSession(existingUser, request);
    }

    @Test
    void linksNewOauthIdentityToExistingUserWhenProviderEmailIsVerified() {
        UserEntity existingUser = activeUser();
        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, true, "Alice", "Existing");
        stubNoIdentity();
        stubUser(existingUser);
        stubToken(existingUser);
        UserAuthenticationResponse response = handle();
        assertThat(response.getToken()).isEqualTo("access-token");
        ArgumentCaptor<OAuthIdentityEntity> savedIdentities = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(oAuthIdentityRepository).save(savedIdentities.capture());
        OAuthIdentityEntity savedIdentity = savedIdentities.getValue();
        assertThat(savedIdentity.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(savedIdentity.getProviderSubject()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(savedIdentity.getEmail()).isEqualTo(EXISTING_EMAIL);
        assertThat(savedIdentity.getUser()).isSameAs(existingUser);
    }

    @Test
    void normalizesProviderSubjectAndEmailBeforeLookupAndIdentitySave() {
        UserEntity existingUser = activeUser();
        stubProfile("  google-subject  ", " Existing@Example.com ", true, "Alice", "Existing");
        stubNoIdentity();
        stubUser(existingUser);
        stubToken(existingUser);
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
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTokenService.issueForNewSession(any(UserEntity.class), eq(request))).thenReturn(tokenPair());
        UserAuthenticationResponse response = handle();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        ArgumentCaptor<UserEntity> savedUsers = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(savedUsers.capture());
        UserEntity savedUser = savedUsers.getValue();
        assertThat(savedUser.getFirstName()).isEqualTo("New");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getEmail()).isEqualTo(NEW_EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo("encoded-random-password");
        assertThat(savedUser.isOauthUser()).isTrue();
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getAuthorities())
                .singleElement()
                .extracting(UserGrantedAuthority::getAuthority)
                .isEqualTo(Authority.USER.name());
        verify(sessionTokenService).issueForNewSession(savedUser, request);
        ArgumentCaptor<OAuthIdentityEntity> savedIdentities = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
        verify(oAuthIdentityRepository).save(savedIdentities.capture());
        assertThat(savedIdentities.getValue().getProviderSubject()).isEqualTo(GOOGLE_SUBJECT);
        assertThat(savedIdentities.getValue().getUser()).isSameAs(savedUser);
    }

    @Test
    void createsNewUserWhenUnverifiedProviderEmailDoesNotExistLocally() {
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, false, "New", "User");
        stubNoIdentity();
        stubNoUser();
        stubNewUserSave();
        handle();
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void usesSafeFallbackNamesWhenProviderProfileHasNoFirstOrLastName() {
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, true, " ", null);
        stubNoIdentity();
        stubNoUser();
        stubNewUserSave();
        handle();
        ArgumentCaptor<UserEntity> savedUsers = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(savedUsers.capture());
        assertThat(savedUsers.getValue().getFirstName()).isEqualTo("OAuth");
        assertThat(savedUsers.getValue().getLastName()).isEqualTo("User");
    }

    @Test
    void trimsAndCapsProviderNamesBeforeCreatingLocalUser() {
        String longName = "x".repeat(140);
        stubProfile(GOOGLE_SUBJECT, NEW_EMAIL, true, "  Ada  ", longName);
        stubNoIdentity();
        stubNoUser();
        stubNewUserSave();
        handle();
        ArgumentCaptor<UserEntity> savedUsers = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(savedUsers.capture());
        assertThat(savedUsers.getValue().getFirstName()).isEqualTo("Ada");
        assertThat(savedUsers.getValue().getLastName()).hasSize(128);
    }

    @Test
    void rejectsProviderAccountsWithTooLongSubject() {
        stubProfile("s".repeat(256), "email@example.com", true, "Long", "Subject");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account subject is too long.");

        verifyNoInteractions(oAuthIdentityRepository, userRepository, sessionTokenService);
    }

    @Test
    void rejectsProviderAccountsWithTooLongEmail() {
        stubProfile(GOOGLE_SUBJECT, "a".repeat(245) + "@example.com", true, "Long", "Email");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account email is too long.");

        verifyNoInteractions(oAuthIdentityRepository, userRepository, sessionTokenService);
    }

    @Test
    void rejectsLinkingWhenUnverifiedProviderEmailExistsLocally() {
        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, false, "New", "User");
        stubNoIdentity();
        stubUser(UserEntity.builder().email(EXISTING_EMAIL).build());

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account email is not verified.");
    }

    @Test
    void rejectsExistingOauthIdentityWhenLocalUserCannotSignIn() {
        UserEntity lockedUser = activeUser();
        lockedUser.setAccountNonLocked(false);

        stubProfile(GOOGLE_SUBJECT, EXISTING_EMAIL, true, "Alice", "Existing");
        stubIdentity(lockedUser);

        assertThatThrownBy(this::handle)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("OAuth account is not available.");
    }

    @Test
    void rejectsEmailLinkedOauthLoginWhenLocalUserCannotSignIn() {
        UserEntity disabledUser = activeUser();
        disabledUser.setEnabled(false);

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

        verifyNoInteractions(oAuthIdentityRepository, userRepository, sessionTokenService);
    }

    @Test
    void rejectsProviderAccountsWithoutAnEmailAddress() {
        stubProfile(GOOGLE_SUBJECT, " ", true, "No", "Email");

        assertThatThrownBy(this::handle)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("google account has no email.");

        verifyNoInteractions(oAuthIdentityRepository, userRepository, sessionTokenService);
    }

    @Test
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

    private UserAuthenticationResponse handle() {
        return service.handle(OAuthProvider.GOOGLE, AUTH_CODE, request);
    }

    private void stubProfile(String providerSubject,
                             String email,
                             boolean emailVerified,
                             String firstName,
                             String lastName) {
        when(providerClient.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(providerClient.exchangeCode(AUTH_CODE))
                .thenReturn(profile(providerSubject, email, emailVerified, firstName, lastName));
    }

    private void stubNoIdentity() {
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, OAuthLoginServiceTest.GOOGLE_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void stubIdentity(UserEntity user) {
        OAuthIdentityEntity identity = OAuthIdentityEntity.builder()
                .provider(OAuthProvider.GOOGLE)
                .providerSubject(OAuthLoginServiceTest.GOOGLE_SUBJECT)
                .email(OAuthLoginServiceTest.EXISTING_EMAIL)
                .user(user)
                .build();
        when(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, OAuthLoginServiceTest.GOOGLE_SUBJECT))
                .thenReturn(Optional.of(identity));
    }

    private void stubUser(UserEntity user) {
        when(userRepository.findByEmail(OAuthLoginServiceTest.EXISTING_EMAIL)).thenReturn(Optional.of(user));
    }

    private void stubNoUser() {
        when(userRepository.findByEmail(OAuthLoginServiceTest.NEW_EMAIL)).thenReturn(Optional.empty());
    }

    private void stubToken(UserEntity user) {
        when(sessionTokenService.issueForNewSession(user, request)).thenReturn(tokenPair());
    }

    private void stubNewUserSave() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionTokenService.issueForNewSession(any(UserEntity.class), eq(request))).thenReturn(tokenPair());
    }

    private static UserAuthenticationResponse tokenPair() {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken("access-token");
        response.setRefreshToken("refresh-token");
        return response;
    }

    private static UserEntity activeUser() {
        return UserEntity.builder()
                .email(OAuthLoginServiceTest.EXISTING_EMAIL)
                .password("secret")
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
    }

    private static OAuthProfile profile(String providerSubject,
                                        String email,
                                        boolean emailVerified,
                                        String firstName,
                                        String lastName) {
        return new OAuthProfile(providerSubject, email, emailVerified, firstName, lastName);
    }
}
