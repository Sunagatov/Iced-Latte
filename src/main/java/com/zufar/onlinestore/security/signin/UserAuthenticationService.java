package com.zufar.onlinestore.security.signin;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
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

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        String jwtToken;
        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            jwtToken = jwtTokenProvider.generateToken(userDetails);

        } catch (DisabledException exception) {
            //  must be thrown if an account is disabled and the AuthenticationManager can test for this state.

        } catch (LockedException exception) {
            // must be thrown if an account is locked and the AuthenticationManager can test for account locking.

        } catch (BadCredentialsException exception) {
            // must be thrown if incorrect credentials are presented. Whilst the above exceptions are optional, an AuthenticationManager must always test credentials.

        }
        return new UserAuthenticationResponse(jwtToken);
    }
}
