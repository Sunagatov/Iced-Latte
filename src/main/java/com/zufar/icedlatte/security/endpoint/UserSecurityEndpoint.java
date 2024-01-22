package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.security.api.UserAuthenticationService;
import com.zufar.icedlatte.security.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.security.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.security.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.dto.UserRegistrationResponse;
import com.zufar.icedlatte.security.jwt.JwtAuthenticationProvider;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final JwtBlacklistValidator jwtBlacklistValidator;
    private final EmailTokenSender emailTokenSender;
    private final EmailTokenConformer emailTokenConformer;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final UserDetailsService userDetailsService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody final UserRegistrationRequest request) {
        log.info("Received registration request for user with email = '{}'", request.email());
        emailTokenSender.sendEmailVerificationCode(request);
        log.info("Email verification token sent to the user with email = '{}'", request.email());
        return ResponseEntity.ok()
                .body(String.format("Email verification token sent to the user with email = %s\nIf You don't receive an email, please check your spam or may be the email address is incorrect", request.email()));
    }

    @Override
    @PostMapping(value = "/confirm")
    public ResponseEntity<UserRegistrationResponse> confirmEmail(@RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        log.info("Received email confirmation request");
        UserRegistrationResponse registrationResponse = emailTokenConformer.confirmEmailByCode(confirmEmailRequest);
        log.info("Email verification completed");
        return new ResponseEntity<>(registrationResponse, HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@RequestBody final UserAuthenticationRequest request) {
        log.info("Received authentication request for user with email = '{}'", request.email());
        UserAuthenticationResponse authenticationResponse = userAuthenticationService.authenticate(request);
        log.info("Authentication completed for user with email = '{}'", request.email());
        return new ResponseEntity<>(authenticationResponse, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserAuthenticationResponse> refresh(@NonNull final HttpServletRequest httpRequest) {
        log.info("Received refresh token request for user");
        var authenticationToken = jwtAuthenticationProvider.get(httpRequest);
        UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationToken.getName());
        UserAuthenticationResponse authenticationResponse = userAuthenticationService.authenticate(userDetails, authenticationToken.getName());
        log.info("Refresh completed for user");
        return new ResponseEntity<>(authenticationResponse, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        log.info("Received logout request");

        String token = jwtTokenFromAuthHeaderExtractor.extract(request);

        jwtBlacklistValidator.addToBlacklist(token);

        return ResponseEntity.ok()
                .body("{ \"message\": \"Logout is successful\" }");
    }
}