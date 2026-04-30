package com.zufar.icedlatte.auth.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.api.SessionTokenService;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;


@Slf4j
@Service
@ConditionalOnProperty(name = "google.enabled", havingValue = "true")
@RequiredArgsConstructor
public class GoogleAuthCallbackHandler {

    private final GoogleTokenExchanger googleTokenExchanger;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    public UserAuthenticationResponse handle(String authorizationCode,
                                             HttpServletRequest httpRequest) throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = googleTokenExchanger.exchange(authorizationCode);

        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Google account has no email");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser((String) payload.get("given_name"), (String) payload.get("family_name"), email));

        return sessionTokenService.issueForNewSession(user, httpRequest);
    }

    private UserEntity createUser(String firstName, String lastName, String email) {
        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
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
        log.info("user.registered.google: userId={}", saved.getId());
        return saved;
    }
}
