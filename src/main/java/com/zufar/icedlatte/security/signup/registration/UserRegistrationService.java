package com.zufar.icedlatte.security.signup.registration;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;
import com.zufar.icedlatte.security.signin.exception.UserRegistrationException;
import com.zufar.icedlatte.security.util.EmailNormalizer;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserRegistrationApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRegistrationApi userRegistrationApi;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    @Transactional(readOnly = true)
    public void ensureEmailAvailable(final UserRegistrationRequest userRegistrationRequest) {
        String email = EmailNormalizer.normalize(userRegistrationRequest.getEmail());
        if (userRegistrationApi.existsByEmail(email)) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw duplicateEmailException();
        }
    }

    @Transactional
    public UserAuthenticationResponse register(
            final UserRegistrationRequest userRegistrationRequest, final HttpServletRequest httpRequest) {
        String email = EmailNormalizer.normalize(userRegistrationRequest.getEmail());
        String encryptedPassword =
                Objects.requireNonNull(passwordEncoder.encode(userRegistrationRequest.getPassword()));

        try {
            UserAuthenticationSnapshot snapshot = userRegistrationApi.registerPasswordUser(
                    userRegistrationRequest.getFirstName(),
                    userRegistrationRequest.getLastName(),
                    email,
                    encryptedPassword);
            log.info("auth.registration.succeeded: userId={}", snapshot.userId());
            return sessionTokenService.issueForNewSession(SecurityUserDetails.from(snapshot), httpRequest);
        } catch (DataIntegrityViolationException e) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw duplicateEmailException(e);
        }
    }

    private static UserRegistrationException duplicateEmailException() {
        return duplicateEmailException(null);
    }

    private static UserRegistrationException duplicateEmailException(Throwable cause) {
        String message = "This email is already registered. Please sign in or use a different email.";
        return cause == null ? new UserRegistrationException(message) : new UserRegistrationException(message, cause);
    }
}
