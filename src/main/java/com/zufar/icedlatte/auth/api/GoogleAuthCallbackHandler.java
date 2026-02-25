package com.zufar.icedlatte.auth.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthCallbackHandler {

    private final GoogleTokenExchanger googleTokenExchanger;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticationResponse handle(String authorizationCode) throws GeneralSecurityException, IOException {
        GoogleIdToken.Payload payload = googleTokenExchanger.exchange(authorizationCode);

        String email = (String) payload.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Google account has no email");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser((String) payload.get("given_name"), (String) payload.get("family_name"), email));

        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(jwtTokenProvider.generateToken(user));
        response.setRefreshToken(jwtTokenProvider.generateRefreshToken(user));
        return response;
    }

    private UserEntity createUser(String firstName, String lastName, String email) {
        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .authorities(Set.of(UserGrantedAuthority.builder().authority(Authority.USER).build()))
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
        UserEntity saved = userRepository.save(user);
        log.info("user.registered.google: userId={}", saved.getId());
        return saved;
    }
}
