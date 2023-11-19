package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.security.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final LoginFailureHandler loginFailureHandler;
    private final ResetLoginAttemptsService resetLoginAttemptsService;

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        String userEmail = request.email();
        String userPassword = request.password();
        int userAccountLockoutDurationMinutes = 30;

        log.info("Authenticating user with email = '{}'", userEmail);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, userPassword)
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String jwtToken = jwtTokenProvider.generateToken(userDetails);
            log.info("Generated JWT token for user with email = '{}'", request.email());

            resetLoginAttemptsService.reset(userEmail);

            return new UserAuthenticationResponse(jwtToken);

        } catch (BadCredentialsException exception) {
            log.error("Invalid credentials for user's account with email = '{}'", userEmail);
            loginFailureHandler.handle(userEmail);
            throw new BadCredentialsException(String.format("Invalid credentials for user's account with email = '%s'", userEmail), exception);

        } catch (LockedException exception) {
            log.error("User's account with email = '{}' is locked", userEmail);
            throw new UserAccountLockedException(userEmail, userAccountLockoutDurationMinutes);

        } catch (Exception exception) {
            log.error("Error occurred during authentication", exception);
            throw exception;
        }
    }
}
