package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
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

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RegistrationDtoConverter registrationDtoConverter;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    @Transactional(readOnly = true)
    public void ensureEmailAvailable(final UserRegistrationRequest userRegistrationRequest) {
        String email = normalizeEmail(userRegistrationRequest.getEmail());
        if (userRepository.existsByEmail(email)) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw duplicateEmailException();
        }
    }

    @Transactional
    public UserAuthenticationResponse register(final UserRegistrationRequest userRegistrationRequest,
                                               final HttpServletRequest httpRequest) {
        String email = normalizeEmail(userRegistrationRequest.getEmail());
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
            log.info("auth.registration.succeeded: userId={}", userEntity.getId());
            return sessionTokenService.issueForNewSession(userEntity, httpRequest);
        } catch (DataIntegrityViolationException e) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw duplicateEmailException(e);
        }
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }

    private static UserRegistrationException duplicateEmailException() {
        return duplicateEmailException(null);
    }

    private static UserRegistrationException duplicateEmailException(Throwable cause) {
        String message = "This email is already registered. Please sign in or use a different email.";
        return cause == null ? new UserRegistrationException(message) : new UserRegistrationException(message, cause);
    }
}
