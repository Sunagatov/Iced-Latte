package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptManager loginAttemptManager;

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        String email = request.email();
        String password = request.password();

        loginAttemptManager.validateUserLoginLockout(email);

        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);

            authenticationManager.authenticate(authenticationToken);
        } catch (Exception exception) {
            // Authentication failed, increment login attempts and lock the account if necessary
            loginAttemptManager.handleFailedLogin(email);
            throw exception;
        }

        // Authentication succeeded, reset login attempts
        loginAttemptManager.resetFailedLoginAttempts(email);

        UserEntity user = (UserEntity) userDetailsService.loadUserByUsername(email);

        String jwtToken = jwtTokenProvider.generateToken(user);

        return new UserAuthenticationResponse(jwtToken);
    }
}
