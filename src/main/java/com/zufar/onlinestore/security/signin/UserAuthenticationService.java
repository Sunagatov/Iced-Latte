package com.zufar.onlinestore.security.signin;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.security.signin.attempts.FailedLoginAttemptHandler;
import com.zufar.onlinestore.security.signin.attempts.ResetLoginAttemptsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final FailedLoginAttemptHandler failedLoginAttemptHandler;
    private final ResetLoginAttemptsService resetLoginAttemptsService;

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        log.info("Authenticating user with email = '{}'", request.email());

        String userEmail = request.email();
        String userPassword = request.password();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userEmail, userPassword)
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String jwtToken = jwtTokenProvider.generateToken(userDetails);
            log.info("Generated JWT token for user with email = '{}'", request.email());

            resetLoginAttemptsService.reset(userEmail);


            return new UserAuthenticationResponse(jwtToken);

        } catch (LockedException exception) {
            log.warn("Account is locked for email: {}", userEmail);
            throw new UserAccountLockedException(userEmail);

        } catch (BadCredentialsException exception) {
            failedLoginAttemptHandler.handle(userEmail);
            throw exception;

        } catch (Exception exception) {
            log.error("Error occurred during authentication", exception);
            throw exception;

        }
    }
}
