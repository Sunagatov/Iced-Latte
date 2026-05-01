package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    @Value("${login-attempts.lockout-duration-minutes}")
    private int userAccountLockoutDurationMinutes;

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final LoginFailureHandler loginFailureHandler;
    private final ResetLoginAttemptsService resetLoginAttemptsService;

    public UserDetails verifyCredentials(final UserAuthenticationRequest request) {
        String userEmail = request.getEmail();
        String userPassword = request.getPassword();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, userPassword)
            );
            if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
                // amazonq-ignore-next-line
                throw new InvalidCredentialsException();
            }
            return userDetails;
        } catch (UsernameNotFoundException exception) {
            // Unknown email — do not persist a DB row for a non-existent account.
            // Rate limiting via PreAuthRateLimitingFilter still applies.
            log.debug("auth.failed: reason=user_not_found");
            throw new InvalidCredentialsException(exception);
        } catch (BadCredentialsException exception) {
            loginFailureHandler.handle(userEmail);
            throw new InvalidCredentialsException(exception);
        } catch (LockedException exception) {
            log.debug("auth.failed: reason=account_locked");
            throw new UserAccountLockedException(userAccountLockoutDurationMinutes);
        } catch (AuthenticationException exception) {
            log.error("auth.error: exceptionClass={}", exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }

    public UserAuthenticationResponse buildTokenPair(final UserDetails userDetails,
                                                     UUID sessionId, String refreshToken) {
        String accessToken = jwtTokenProvider.generateToken(userDetails, sessionId);
        log.info("auth.sign_in.succeeded: sessionId={}", maskSessionId(sessionId));
        resetLoginAttemptsService.reset(userDetails.getUsername());
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    private static String maskSessionId(UUID sessionId) {
        if (sessionId == null) {
            return "unknown";
        }
        String value = sessionId.toString();
        return StringUtils.left(StringUtils.overlay(value, "****", 6, value.length()), 10);
    }
}
