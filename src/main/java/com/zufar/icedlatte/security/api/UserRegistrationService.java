package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RegistrationDtoConverter registrationDtoConverter;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final JwtBlacklistService jwtBlacklistService;

    @Transactional
    public UserAuthenticationResponse register(final UserRegistrationRequest userRegistrationRequest,
                                               final HttpServletRequest httpRequest) {
        String email = userRegistrationRequest.getEmail().toLowerCase(java.util.Locale.ROOT).trim();
        String encryptedPassword = passwordEncoder.encode(userRegistrationRequest.getPassword());
        UserGrantedAuthority defaultUserGrantedAuthority = UserGrantedAuthority.builder().authority(Authority.USER).build();

        UserEntity newUserEntity = registrationDtoConverter.toEntity(userRegistrationRequest);
        newUserEntity.setEmail(email);
        newUserEntity.setPassword(encryptedPassword);
        newUserEntity.addAuthority(defaultUserGrantedAuthority);
        newUserEntity.setAccountNonExpired(true);
        newUserEntity.setAccountNonLocked(true);
        newUserEntity.setCredentialsNonExpired(true);
        newUserEntity.setEnabled(true);

        try {
            UserEntity userEntity = userRepository.saveAndFlush(newUserEntity);
            java.util.UUID sessionId = java.util.UUID.randomUUID();
            final String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userEntity, sessionId);
            authSessionService.createSession(sessionId, userEntity.getId(), jwtBlacklistService.sha256(jwtRefreshToken), httpRequest);
            final String jwtToken = jwtTokenProvider.generateToken(userEntity, sessionId);
            log.info("auth.registration.succeeded: userId={}", userEntity.getId());
            UserAuthenticationResponse response = new UserAuthenticationResponse();
            response.setToken(jwtToken);
            response.setRefreshToken(jwtRefreshToken);
            // amazonq-ignore-next-line
            return response;
        } catch (DataIntegrityViolationException e) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw new UserRegistrationException("Email already registered.", e);
        }
    }
}
