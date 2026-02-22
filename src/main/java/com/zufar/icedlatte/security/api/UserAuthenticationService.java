package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        String userEmail = request.getEmail();
        String userPassword = request.getPassword();

        log.info("auth.authenticating");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, userPassword)
            );
            if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
                // amazonq-ignore-next-line
                throw new InvalidCredentialsException();
            }
            return buildResponse(userDetails, userEmail);

        } catch (UsernameNotFoundException exception) {
            log.warn("auth.failed: reason=user_not_found", exception);
            throw new InvalidCredentialsException(exception);
        } catch (BadCredentialsException exception) {
            log.warn("auth.failed: reason=invalid_credentials", exception);
            loginFailureHandler.handle(userEmail);
            throw new InvalidCredentialsException(exception);
        } catch (LockedException exception) {
            log.warn("auth.failed: reason=account_locked", exception);
            throw new UserAccountLockedException(userAccountLockoutDurationMinutes);
        } catch (AuthenticationException exception) {
            log.error("auth.error", exception);
            throw exception;
        }
    }
// amazonq-ignore-next-line

    public UserAuthenticationResponse authenticate(final UserDetails userDetails, String userEmail) {
        return buildResponse(userDetails, userEmail);
    }

    private UserAuthenticationResponse buildResponse(UserDetails userDetails, String userEmail) {
        String jwtToken = jwtTokenProvider.generateToken(userDetails);
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
        log.debug("auth.token.generated");
        resetLoginAttemptsService.reset(userEmail);
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(jwtToken);
        response.setRefreshToken(jwtRefreshToken);
        return response;
    }
}
