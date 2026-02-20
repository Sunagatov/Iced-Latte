package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationResponse;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.security.api.UserAuthenticationService;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtRefreshTokenValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import com.zufar.icedlatte.user.api.ChangeUserPasswordOperationPerformer;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
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
    private final JwtRefreshTokenValidator jwtRefreshTokenValidator;
    private final UserDetailsService userDetailsService;
    private final SingleUserProvider singleUserProvider;
    private final ChangeUserPasswordOperationPerformer changeUserPasswordOperationPerformer;

    private final HttpServletRequest httpRequest;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid final UserRegistrationRequest request) {
        var email = request.getEmail();
        log.info("Registering user: {}", email);
        emailTokenSender.sendEmailVerificationCode(request);
        log.info("Verification email sent to: {}", email);
        return ResponseEntity.ok(
                String.format("Email verification token sent to the user with email = %s",
                        StringEscapeUtils.escapeJava(email)));
    }

    @Override
    @PostMapping(value = "/confirm")
    public ResponseEntity<UserRegistrationResponse> confirmEmail(@Validated @Valid @RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        log.info("Confirming email verification");
        var response = emailTokenConformer.confirmEmailByCode(confirmEmailRequest);
        log.info("Email verification completed");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@Valid @RequestBody final UserAuthenticationRequest request) {
        log.info("Authenticating user: {}", request.getEmail());
        var response = userAuthenticationService.authenticate(request);
        log.info("Authentication completed for: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthenticationResponse> refreshToken() {
        log.info("Refreshing token");
        String userEmail = jwtRefreshTokenValidator.extractEmail(httpRequest);
        var userDetails = userDetailsService.loadUserByUsername(userEmail);
        var response = userAuthenticationService.authenticate(userDetails, userEmail);
        log.info("Token refreshed for: {}", userEmail);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Processing logout request");
        
        String authHeader = httpRequest.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader)) {
            log.debug("Logout request without Authorization header - treating as successful");
            return ResponseEntity.ok().build();
        }
        
        try {
            String token = jwtTokenFromAuthHeaderExtractor.extract(authHeader);
            jwtBlacklistValidator.addToBlacklist(token);
            log.info("User logout completed successfully");
        } catch (com.zufar.icedlatte.security.exception.AbsentBearerHeaderException ex) {
            log.warn("Failed to extract token during logout: {}", ex.getMessage(), ex);
            // Still return success to prevent information leakage
        } catch (IllegalStateException ex) {
            log.warn("Failed to blacklist token during logout: {}", ex.getMessage(), ex);
            // Still return success to prevent information leakage
        }
        
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
        log.info("Processing forgot password request");
        try {
            var user = singleUserProvider.getUserByEmail(request.getEmail());
            var verificationRequest = new UserRegistrationRequest(user.getFirstName(), user.getLastName(), user.getEmail(), "");
            emailTokenSender.sendEmailVerificationCode(verificationRequest);
        } catch (UserNotFoundException e) {
            log.warn("Forgot password requested for unknown email");
        }
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody final ChangePasswordRequest request) {
        log.info("Changing password for: {}", request.getEmail());
        var user = singleUserProvider.getUserByEmail(request.getEmail());
        emailTokenConformer.confirmResetPasswordEmailByCode(new ConfirmEmailRequest(request.getCode()));
        changeUserPasswordOperationPerformer.changeUserPassword(user.getId(), request.getPassword());
        log.info("Password changed for: {}", request.getEmail());
        return ResponseEntity.ok().build();
    }
}
