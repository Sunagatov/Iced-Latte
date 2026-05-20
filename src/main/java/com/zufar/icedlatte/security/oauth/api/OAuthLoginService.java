package com.zufar.icedlatte.security.oauth.api;

import com.zufar.icedlatte.security.oauth.entity.OAuthIdentityEntity;
import com.zufar.icedlatte.security.oauth.repository.OAuthIdentityRepository;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.api.session.SessionTokenService;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PROVIDER_SUBJECT_LENGTH = 255;
    private static final int MAX_NAME_LENGTH = 128;

    private final List<OAuthProviderClient> providerClients;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    public Optional<OAuthProviderClient> findClient(OAuthProvider provider) {
        return providerClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst();
    }

    @Transactional
    public UserAuthenticationResponse handle(OAuthProvider provider,
                                             String authorizationCode,
                                             HttpServletRequest httpRequest) {
        OAuthProviderClient client = findClient(provider)
                .orElseThrow(() -> new BadRequestException("OAuth provider is not available."));
        OAuthProfile profile = client.exchangeCode(authorizationCode);

        String providerSubject = normalizeRequired(profile.providerSubject());
        if (providerSubject == null || providerSubject.isBlank()) {
            throw new BadRequestException(provider.id() + " account has no subject.");
        }
        if (providerSubject.length() > MAX_PROVIDER_SUBJECT_LENGTH) {
            throw new BadRequestException(provider.id() + " account subject is too long.");
        }

        String email = normalizeEmail(profile.email());
        if (email == null || email.isBlank()) {
            throw new BadRequestException(provider.id() + " account has no email.");
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new BadRequestException(provider.id() + " account email is too long.");
        }

        Optional<OAuthIdentityEntity> existingIdentity =
                oAuthIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject);
        UserEntity user = existingIdentity
                .map(OAuthIdentityEntity::getUser)
                .orElseGet(() -> findOrCreateUserAndIdentity(provider, profile, providerSubject, email));
        ensureUserCanSignIn(user);

        return sessionTokenService.issueForNewSession(user, httpRequest);
    }

    private UserEntity findOrCreateUserAndIdentity(OAuthProvider provider,
                                                   OAuthProfile profile,
                                                   String providerSubject,
                                                   String email) {
        Optional<UserEntity> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && !profile.emailVerified()) {
            throw new BadRequestException(provider.id() + " account email is not verified.");
        }
        UserEntity user = existingUser
                .orElseGet(() -> createUser(provider, profile, email));
        oAuthIdentityRepository.save(OAuthIdentityEntity.builder()
                .provider(provider)
                .providerSubject(providerSubject)
                .email(email)
                .user(user)
                .build());
        return user;
    }

    private UserEntity createUser(OAuthProvider provider,
                                  OAuthProfile profile,
                                  String email) {
        UserEntity user = UserEntity.builder()
                .firstName(defaultName(profile.firstName(), "OAuth"))
                .lastName(defaultName(profile.lastName(), "User"))
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .oauthUser(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
        user.addAuthority(UserGrantedAuthority.builder()
                .authority(Authority.USER)
                .build());
        UserEntity saved = userRepository.save(user);
        log.info("user.registered.oauth: provider={}, userId={}", provider.id(), saved.getId());
        return saved;
    }

    private void ensureUserCanSignIn(UserEntity user) {
        if (!user.isEnabled()
                || !user.isAccountNonLocked()
                || !user.isAccountNonExpired()
                || !user.isCredentialsNonExpired()) {
            throw new UnauthorizedException("OAuth account is not available.");
        }
    }

    private String defaultName(String value,
                               String fallback) {
        String normalized = normalizeRequired(value);
        if (normalized == null || normalized.isBlank()) {
            return fallback;
        }
        return normalized.length() > MAX_NAME_LENGTH ? normalized.substring(0, MAX_NAME_LENGTH) : normalized;
    }

    private String normalizeEmail(String email) {
        String value = normalizeRequired(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }
}
