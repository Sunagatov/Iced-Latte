package com.zufar.onlinestore.security.signin;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.security.signin.attempts.FailedLoginHandler;
import com.zufar.onlinestore.security.signin.attempts.ResetLoginAttemptsService;
import com.zufar.onlinestore.security.signin.attempts.UserLockoutValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
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
    private final FailedLoginHandler failedLoginHandler;
    private final ResetLoginAttemptsService resetLoginAttemptsService;
    private final UserLockoutValidator userLockoutValidator;

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        String email = request.email();
        String password = request.password();

        userLockoutValidator.validate(email);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (Exception exception) {
            failedLoginHandler.handle(email);
            throw exception;
        }

        resetLoginAttemptsService.reset(email);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwtToken = jwtTokenProvider.generateToken(userDetails);

        return new UserAuthenticationResponse(jwtToken);
    }
}
