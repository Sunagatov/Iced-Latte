package com.zufar.icedlatte.security.signup.registration;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;
import com.zufar.icedlatte.security.signin.exception.UserRegistrationException;
import com.zufar.icedlatte.security.signin.turnstile.TurnstileVerifier;
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
    private final TurnstileVerifier turnstileVerifier;

    @Transactional(readOnly = true)
    public void ensureRegistrationAllowed(final UserRegistrationRequest userRegistrationRequest) {
        turnstileVerifier.verify(userRegistrationRequest.getTurnstileToken());
        ensureEmailAvailable(userRegistrationRequest);
    }

    @Transactional
    public AuthenticationTokens register(
            final UserRegistrationRequest userRegistrationRequest, final HttpServletRequest httpRequest) {
        ensureRegistrationAllowed(userRegistrationRequest);
        String encryptedPassword =
                Objects.requireNonNull(passwordEncoder.encode(userRegistrationRequest.getPassword()));
        return persistPasswordUser(userRegistrationRequest, encryptedPassword, httpRequest);
    }

    @Transactional
    public AuthenticationTokens completeEmailVerifiedRegistration(
            final UserRegistrationRequest userRegistrationRequest,
            final String encodedPassword,
            final HttpServletRequest httpRequest) {
        return persistPasswordUser(userRegistrationRequest, encodedPassword, httpRequest);
    }

    private AuthenticationTokens persistPasswordUser(
            final UserRegistrationRequest userRegistrationRequest,
            final String encodedPassword,
            final HttpServletRequest httpRequest) {
        String email = EmailNormalizer.normalize(userRegistrationRequest.getEmail());

        try {
            UserAuthenticationSnapshot snapshot = userRegistrationApi.registerPasswordUser(
                    userRegistrationRequest.getFirstName(),
                    userRegistrationRequest.getLastName(),
                    email,
                    Objects.requireNonNull(encodedPassword));
            log.info("auth.registration.succeeded: userId={}", snapshot.userId());
            return sessionTokenService.issueForNewSession(SecurityUserDetails.from(snapshot), httpRequest);
        } catch (DataIntegrityViolationException e) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw duplicateEmailException(e);
        }
    }

    private void ensureEmailAvailable(final UserRegistrationRequest userRegistrationRequest) {
        String email = EmailNormalizer.normalize(userRegistrationRequest.getEmail());
        if (userRegistrationApi.existsByEmail(email)) {
            log.warn("auth.registration.failed: reason=email_already_registered");
            throw duplicateEmailException();
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
