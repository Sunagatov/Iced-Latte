package com.zufar.icedlatte.security.signup.registration;

import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;
import com.zufar.icedlatte.security.signin.exception.UserRegistrationException;
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

    public void ensureRegistrationAllowed(final UserRegistrationRequest userRegistrationRequest) {
        turnstileVerifier.verify(userRegistrationRequest.getTurnstileToken());
        ensureEmailAvailable(userRegistrationRequest);
    }

    @Transactional
    public AuthenticationTokens register(
            final UserRegistrationRequest userRegistrationRequest, final AuthSessionRequestMetadata requestMetadata) {
        ensureRegistrationAllowed(userRegistrationRequest);
        String encryptedPassword =
                Objects.requireNonNull(passwordEncoder.encode(userRegistrationRequest.getPassword()));
        return persistPasswordUser(userRegistrationRequest, encryptedPassword, requestMetadata);
    }

    @Transactional
    public AuthenticationTokens completeEmailVerifiedRegistration(
            final UserRegistrationRequest userRegistrationRequest,
            final String encodedPassword,
            final AuthSessionRequestMetadata requestMetadata) {
        return persistPasswordUser(userRegistrationRequest, encodedPassword, requestMetadata);
    }

    private AuthenticationTokens persistPasswordUser(
            final UserRegistrationRequest userRegistrationRequest,
            final String encodedPassword,
            final AuthSessionRequestMetadata requestMetadata) {
        String email = EmailNormalizer.normalize(userRegistrationRequest.getEmail());

        try {
            UserAuthenticationSnapshot snapshot = userRegistrationApi.registerPasswordUser(
                    userRegistrationRequest.getFirstName(),
                    userRegistrationRequest.getLastName(),
                    email,
                    Objects.requireNonNull(encodedPassword));
            log.info("auth.registration.succeeded: userId={}", snapshot.userId());
            SecurityUserDetails securityUserDetails = SecurityUserDetails.from(snapshot);
            return sessionTokenService.issueForNewSession(securityUserDetails, requestMetadata);
        } catch (DataIntegrityViolationException e) {
            log.warn("auth.registration.failed: reason=email_already_registered, source=database_constraint");
            throw duplicateEmailException(e);
        }
    }

    private void ensureEmailAvailable(final UserRegistrationRequest userRegistrationRequest) {
        String email = EmailNormalizer.normalize(userRegistrationRequest.getEmail());
        if (userRegistrationApi.existsByEmail(email)) {
            log.warn("auth.registration.failed: reason=email_already_registered, source=preflight_check");
            throw duplicateEmailException();
        }
    }

    private static UserRegistrationException duplicateEmailException() {
        return duplicateEmailException(null);
    }

    private static UserRegistrationException duplicateEmailException(Throwable cause) {
        String message = "This email is already registered. Please sign in or use a different email.";
        if (cause == null) {
            return new UserRegistrationException(message);
        }
        return new UserRegistrationException(message, cause);
    }
}
