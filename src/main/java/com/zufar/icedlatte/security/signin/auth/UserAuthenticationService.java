package com.zufar.icedlatte.security.signin.auth;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.signin.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.signin.lockout.LoginAttemptProperties;
import com.zufar.icedlatte.security.signin.lockout.LoginAttemptService;
import com.zufar.icedlatte.security.signin.turnstile.TurnstileVerifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final SessionTokenService sessionTokenService;
    private final TurnstileVerifier turnstileVerifier;
    private final LoginAttemptProperties loginAttemptProperties;

    public AuthenticationTokens authenticate(
            final UserAuthenticationRequest request, final HttpServletRequest httpRequest) {
        turnstileVerifier.verify(request.getTurnstileToken());
        UserDetails userDetails = verifyCredentials(request);
        loginAttemptService.resetAfterSuccessfulAuthentication(EmailNormalizer.normalize(request.getEmail()));
        return sessionTokenService.issueForNewSession(userDetails, httpRequest);
    }

    public UserDetails verifyCredentials(final UserAuthenticationRequest request) {
        String userEmail = EmailNormalizer.normalize(request.getEmail());
        String userPassword = request.getPassword();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, userPassword));
            if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
                // amazonq-ignore-next-line
                throw new InvalidCredentialsException();
            }
            return userDetails;
        } catch (UsernameNotFoundException | BadCredentialsException exception) {
            loginAttemptService.recordFailure(userEmail);
            throw new InvalidCredentialsException(exception);
        } catch (LockedException exception) {
            log.debug("auth.failed: reason=account_locked");
            throw new UserAccountLockedException(loginAttemptProperties.lockoutDurationMinutes());
        } catch (AuthenticationException exception) {
            log.error("auth.error: exceptionClass={}", exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }
}
