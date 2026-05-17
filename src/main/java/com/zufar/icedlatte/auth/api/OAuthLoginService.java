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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

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

        String providerSubject = profile.providerSubject();
        if (providerSubject == null || providerSubject.isBlank()) {
            throw new BadRequestException(provider.id() + " account has no subject.");
        }

        String email = profile.email();
        if (email == null || email.isBlank()) {
            throw new BadRequestException(provider.id() + " account has no email.");
        }

        Optional<OAuthIdentityEntity> existingIdentity =
                oAuthIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject);
        UserEntity user = existingIdentity
                .map(OAuthIdentityEntity::getUser)
                .orElseGet(() -> findOrCreateUserAndIdentity(provider, profile, providerSubject, email));

        return sessionTokenService.issueForNewSession(user, httpRequest);
    }

    private UserEntity findOrCreateUserAndIdentity(OAuthProvider provider,
                                                   OAuthProfile profile,
                                                   String providerSubject,
                                                   String email) {
        UserEntity user = findLinkableUser(profile, email)
                .orElseGet(() -> createUser(provider, profile, email));
        oAuthIdentityRepository.save(OAuthIdentityEntity.builder()
                .provider(provider)
                .providerSubject(providerSubject)
                .email(email)
                .user(user)
                .build());
        return user;
    }

    private Optional<UserEntity> findLinkableUser(OAuthProfile profile,
                                                  String email) {
        if (!profile.emailVerified()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    private UserEntity createUser(OAuthProvider provider,
                                  OAuthProfile profile,
                                  String email) {
        UserEntity user = UserEntity.builder()
                .firstName(profile.firstName())
                .lastName(profile.lastName())
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
}
