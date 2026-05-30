package com.zufar.icedlatte.security.oauth.login;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;
import com.zufar.icedlatte.security.oauth.entity.OAuthIdentityEntity;
import com.zufar.icedlatte.security.oauth.repository.OAuthIdentityRepository;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserRegistrationApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PROVIDER_SUBJECT_LENGTH = 255;
    private static final int MAX_NAME_LENGTH = 128;

    private final List<OAuthProviderClient> providerClients;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final UserAuthenticationApi userAuthenticationApi;
    private final UserRegistrationApi userRegistrationApi;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    public Optional<OAuthProviderClient> findClient(OAuthProvider provider) {
        return providerClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst();
    }

    @Transactional
    public AuthenticationTokens handle(OAuthProvider provider, String authorizationCode, HttpServletRequest httpRequest) {
        OAuthProviderClient client =
                findClient(provider).orElseThrow(() -> new BadRequestException("OAuth provider is not available."));
        OAuthProfile profile = client.exchangeCode(authorizationCode);

        String providerSubject = normalizeRequired(profile.providerSubject());
        if (providerSubject == null || providerSubject.isBlank()) {
            throw new BadRequestException(provider.id() + " account has no subject.");
        }
        if (providerSubject.length() > MAX_PROVIDER_SUBJECT_LENGTH) {
            throw new BadRequestException(provider.id() + " account subject is too long.");
        }

        String email = EmailNormalizer.normalize(profile.email());
        if (email == null || email.isBlank()) {
            throw new BadRequestException(provider.id() + " account has no email.");
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new BadRequestException(provider.id() + " account email is too long.");
        }
        if (!profile.emailVerified()) {
            throw new BadRequestException(provider.id() + " account email is not verified.");
        }

        Optional<OAuthIdentityEntity> existingIdentity =
                oAuthIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject);
        UserAuthenticationSnapshot user = existingIdentity
                .map(identity -> userAuthenticationApi
                        .findUserAuthenticationById(identity.getUserId())
                        .orElseThrow(() -> new UnauthorizedException("OAuth account is not available.")))
                .orElseGet(() -> findOrCreateUserAndIdentity(provider, profile, providerSubject, email));

        ensureUserCanSignIn(user);

        return sessionTokenService.issueForNewSession(SecurityUserDetails.from(user), httpRequest);
    }

    private UserAuthenticationSnapshot findOrCreateUserAndIdentity(
            OAuthProvider provider, OAuthProfile profile, String providerSubject, String email) {
        Optional<UserAuthenticationSnapshot> existingUser = userAuthenticationApi.findUserAuthenticationByEmail(email);
        UserAuthenticationSnapshot user = existingUser.orElseGet(() -> createUser(provider, profile, email));
        try {
            oAuthIdentityRepository.save(OAuthIdentityEntity.builder()
                    .provider(provider)
                    .providerSubject(providerSubject)
                    .email(email)
                    .userId(user.userId())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            log.info("auth.oauth.identity_race: provider={}", provider.id());
            return loadUserFromExistingIdentity(provider, providerSubject);
        }
        return user;
    }

    private UserAuthenticationSnapshot loadUserFromExistingIdentity(OAuthProvider provider, String providerSubject) {
        OAuthIdentityEntity identity = oAuthIdentityRepository
                .findByProviderAndProviderSubject(provider, providerSubject)
                .orElseThrow(() -> new UnauthorizedException("OAuth account is not available."));
        return userAuthenticationApi
                .findUserAuthenticationById(identity.getUserId())
                .orElseThrow(() -> new UnauthorizedException("OAuth account is not available."));
    }

    private UserAuthenticationSnapshot createUser(OAuthProvider provider, OAuthProfile profile, String email) {
        UserAuthenticationSnapshot saved = userRegistrationApi.registerOAuthUser(
                defaultName(profile.firstName(), "OAuth"),
                defaultName(profile.lastName(), "User"),
                email,
                Objects.requireNonNull(passwordEncoder.encode(UUID.randomUUID().toString())));
        log.info("user.registered.oauth: provider={}, userId={}", provider.id(), saved.userId());
        return saved;
    }

    private void ensureUserCanSignIn(UserAuthenticationSnapshot user) {
        if (!user.enabled() || !user.accountNonLocked() || !user.accountNonExpired() || !user.credentialsNonExpired()) {
            throw new UnauthorizedException("OAuth account is not available.");
        }
    }

    private String defaultName(String value, String fallback) {
        String normalized = normalizeRequired(value);
        if (normalized == null || normalized.isBlank()) {
            return fallback;
        }
        return normalized.length() > MAX_NAME_LENGTH ? normalized.substring(0, MAX_NAME_LENGTH) : normalized;
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }
}
