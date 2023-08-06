package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.dto.authentication.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.authentication.UserAuthenticationResponse;
import com.zufar.onlinestore.security.dto.registration.UserRegistrationRequest;
import com.zufar.onlinestore.security.dto.registration.UserRegistrationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSecurityManager {

    private final UserAuthenticationService userAuthenticationService;
    private final UserRegistrationService userRegistrationService;

    public UserRegistrationResponse register(final UserRegistrationRequest request) {
        return userRegistrationService.register(request);
    }

    public UserAuthenticationResponse authenticate(final UserAuthenticationRequest request) {
        return userAuthenticationService.authenticate(request);
    }

    public void logout(final HttpServletRequest request,
                       final HttpServletResponse response) {
        SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
        securityContextLogoutHandler.logout(request, response, null);
    }
}
