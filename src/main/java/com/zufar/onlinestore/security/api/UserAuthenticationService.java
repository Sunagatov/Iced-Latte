package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.user.api.UserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserApi userApi;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        Authentication authentication = getAuthentication(request);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtTokenProvider.generateToken(userDetails);
        return new UserAuthenticationResponse(jwtToken);
    }

    private Authentication getAuthentication(final UserAuthenticationRequest request) {
        String hashPassword = userApi.getPasswordByEmail(request.email());
        if (passwordEncoder.matches(request.password(), hashPassword)) {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), hashPassword));
        } else {
            throw new BadCredentialsException("Bad credentials");
        }
    }
}
