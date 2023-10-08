package com.zufar.onlinestore.security.signin;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.security.signin.attempts.LoginAttemptManager;
import com.zufar.onlinestore.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptManager loginAttemptManager;

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        String email = request.email();
        String password = request.password();

        loginAttemptManager.validateUserLoginLockout(email);

        Authentication authentication;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);

            authentication = authenticationManager.authenticate(
                    authenticationToken
            );        } catch (Exception exception) {
            // Authentication failed, increment login attempts and lock the account if necessary
            loginAttemptManager.handleFailedLogin(email);
            throw exception;
        }

        // Authentication succeeded, reset login attempts
        loginAttemptManager.resetFailedLoginAttempts(email);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwtToken = jwtTokenProvider.generateToken(userDetails);

        return new UserAuthenticationResponse(jwtToken);
    }
}
