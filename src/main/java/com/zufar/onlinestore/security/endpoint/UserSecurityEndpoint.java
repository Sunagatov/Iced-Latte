package com.zufar.onlinestore.security.endpoint;

import com.zufar.onlinestore.openapi.security.api.SecurityApi;
import com.zufar.onlinestore.security.api.UserAuthenticationService;
import com.zufar.onlinestore.security.api.UserRegistrationService;
import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.dto.UserRegistrationRequest;
import com.zufar.onlinestore.security.dto.UserRegistrationResponse;
import com.zufar.onlinestore.security.jwt.JwtBlacklistValidator;
import com.zufar.onlinestore.security.jwt.JwtTokenFromAuthHeaderExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping(value = UserSecurityEndpoint.USER_SECURITY_API_URL)
@RequiredArgsConstructor
public class UserSecurityEndpoint implements SecurityApi {

    public static final String USER_SECURITY_API_URL = "/api/v1/auth/";

    private final UserAuthenticationService userAuthenticationService;
    private final UserRegistrationService userRegistrationService;
    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final JwtBlacklistValidator jwtBlacklistValidator;

    @Override
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> register(@RequestBody final UserRegistrationRequest request) {
        log.info("Received registration request for user with email = '{}'", request.email());
        UserRegistrationResponse registrationResponse = userRegistrationService.register(request);
        log.info("Registration completed for user with email = '{}'", request.email());
        return new ResponseEntity<>(registrationResponse, HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/confirmation/{token}")
    public ResponseEntity<Void> postUserEmailConfirmation(String token) {
        userRegistrationService.confirmRegistrationEmail(token);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@RequestBody final UserAuthenticationRequest request) {
        log.info("Received authentication request for user with email = '{}'", request.email());
        UserAuthenticationResponse authenticationResponse = userAuthenticationService.authenticate(request);
        log.info("Authentication completed for user with email = '{}'", request.email());
        return ResponseEntity.ok(authenticationResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.info("Received logout request");

        String token = jwtTokenFromAuthHeaderExtractor.extract(request);

        jwtBlacklistValidator.addToBlacklist(token);

        return ResponseEntity.ok()
                .body("{ \"message\": \"Logout is successful\" }");
    }
}
