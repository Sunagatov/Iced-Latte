package com.zufar.onlinestore.security.signin;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.security.signin.attempts.FailedLoginHandler;
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
    private final FailedLoginHandler failedLoginHandler;
    private final ResetLoginAttemptsService resetLoginAttemptsService;

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        String jwtToken;

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            jwtToken = jwtTokenProvider.generateToken(userDetails);

            // If authentication is successful, reset the login attempts for that user
            resetLoginAttemptsService.reset(request.email());

            int i = 0;

        } catch (LockedException exception) {

            // Handle account locked situation
            log.warn("Account is locked for email: {}", request.email());
            throw new UserAccountLockedException(request.email() , exception);

        } catch (BadCredentialsException exception) {

            // Handle failed login attempt
            failedLoginHandler.handle(request.email());
            throw exception; // Re-throwing the exception to maintain existing flow

        } catch (Exception exception) {

            // Handle other exceptions if needed
            log.error("Error occurred during authentication", exception);
            throw exception;

        }

        return new UserAuthenticationResponse(jwtToken);
    }
}
