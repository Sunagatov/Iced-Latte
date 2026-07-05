package com.zufar.icedlatte.security.oauth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;
import com.zufar.icedlatte.security.oauth.entity.OAuthIdentityEntity;
import com.zufar.icedlatte.security.oauth.repository.OAuthIdentityRepository;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.test.config.IntegrationTestBase;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserRegistrationApi;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;

@DisplayName("OAuthLoginService integration tests")
class OAuthLoginServiceIntegrationTest extends IntegrationTestBase {

    private static final String AUTH_CODE = "auth-code";
    private static final String GOOGLE_SUBJECT = "google-subject";
    private static final String EMAIL = "oauth-race@example.com";
    private static final AuthSessionRequestMetadata REQUEST_METADATA =
            new AuthSessionRequestMetadata("TestAgent", "127.0.0.1");

    @Autowired
    private OAuthLoginService service;

    @Autowired
    private OAuthIdentityRepository oAuthIdentityRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private OAuthProviderClientRegistry providerClientRegistry;

    @MockitoBean
    private UserRegistrationApi userRegistrationApi;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SessionTokenService sessionTokenService;

    @BeforeEach
    void setUp() {
        oAuthIdentityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("recovers from duplicate oauth identity creation when the unique violation is raised on flush")
    void recoversFromDuplicateOauthIdentityCreationWhenUniqueViolationIsRaisedOnFlush() {
        OAuthProviderClient client =
                new FakeOAuthProviderClient(new OAuthProfile(GOOGLE_SUBJECT, EMAIL, true, "Race", "Winner"));
        when(providerClientRegistry.findClient(OAuthProvider.GOOGLE)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");

        UserAuthenticationSnapshot[] existingUser = new UserAuthenticationSnapshot[1];
        when(userRegistrationApi.registerOAuthUser("Race", "Winner", EMAIL, "encoded-random-password"))
                .thenAnswer(invocation -> {
                    existingUser[0] = persistUser(EMAIL, "encoded-random-password");
                    oAuthIdentityRepository.saveAndFlush(OAuthIdentityEntity.builder()
                            .provider(OAuthProvider.GOOGLE)
                            .providerSubject(GOOGLE_SUBJECT)
                            .email(EMAIL)
                            .userId(existingUser[0].userId())
                            .build());
                    return existingUser[0];
                });
        when(sessionTokenService.issueForNewSession(any(), eq(REQUEST_METADATA)))
                .thenReturn(new AuthenticationTokens("access-token", "refresh-token"));

        AuthenticationTokens response = service.handle(OAuthProvider.GOOGLE, AUTH_CODE, REQUEST_METADATA);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(oAuthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, GOOGLE_SUBJECT))
                .isPresent()
                .get()
                .extracting(OAuthIdentityEntity::getUserId)
                .isEqualTo(existingUser[0].userId());
    }

    private UserAuthenticationSnapshot persistUser(String email, String encodedPassword) {
        UserEntity user = UserEntity.builder()
                .firstName("Race")
                .lastName("Winner")
                .email(email)
                .password(encodedPassword)
                .oauthUser(true)
                .build();
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        user.addAuthority(
                UserGrantedAuthority.builder().authority(Authority.USER).build());
        UserEntity saved = userRepository.saveAndFlush(user);
        return new UserAuthenticationSnapshot(
                saved.getId(),
                saved.getEmail(),
                saved.getPassword(),
                List.of("USER"),
                saved.isAccountNonExpired(),
                saved.isAccountNonLocked(),
                saved.isCredentialsNonExpired(),
                saved.isEnabled());
    }

    private record FakeOAuthProviderClient(OAuthProfile profile) implements OAuthProviderClient {

        @Override
        public OAuthProvider provider() {
            return OAuthProvider.GOOGLE;
        }

        @Override
        public URI buildAuthorizationUri(String state) {
            return URI.create("https://accounts.google.test/oauth?state=" + state);
        }

        @Override
        public OAuthProfile exchangeCode(String authorizationCode) {
            return profile;
        }
    }
}
