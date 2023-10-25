package com.zufar.onlinestore.security.endpoint;

import com.zufar.onlinestore.openapi.security.api.SecurityApi;
import com.zufar.onlinestore.security.api.UserAuthenticationService;
import com.zufar.onlinestore.security.api.UserRegistrationService;
import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.dto.UserRegistrationRequest;
import com.zufar.onlinestore.security.dto.UserRegistrationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(value = UserSecurityEndpoint.USER_SECURITY_API_URL)
@RequiredArgsConstructor
public class UserSecurityEndpoint implements SecurityApi {

    public static final String USER_SECURITY_API_URL = "/api/v1/auth/";

    private final UserAuthenticationService userAuthenticationService;
    private final UserRegistrationService userRegistrationService;
    @Override
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> register(@RequestBody final UserRegistrationRequest request) {
        log.info("Received registration request for user with email = '{}'", request.email());
        UserRegistrationResponse registrationResponse = userRegistrationService.register(request);
        log.info("Registration completed for user with email = '{}'", request.email());
        return new ResponseEntity<>(registrationResponse, HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@RequestBody final UserAuthenticationRequest request) {
        log.info("Received authentication request for user with email = '{}'", request.email());
        UserAuthenticationResponse authenticationResponse = userAuthenticationService.authenticate(request);
        log.info("Authentication completed for user with email = '{}'", request.email());
        return ResponseEntity.ok(authenticationResponse);
    }
}
