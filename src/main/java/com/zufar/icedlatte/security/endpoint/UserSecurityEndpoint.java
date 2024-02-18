package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.security.api.UserAuthenticationService;
import com.zufar.icedlatte.security.dto.ChangePasswordRequest;
import com.zufar.icedlatte.security.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.security.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.security.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.security.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.dto.UserRegistrationResponse;
import com.zufar.icedlatte.security.jwt.JwtAuthenticationProvider;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import com.zufar.icedlatte.user.api.ChangeUserPasswordOperationPerformer;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final SingleUserProvider singleUserProvider;
    private final ChangeUserPasswordOperationPerformer changeUserPasswordOperationPerformer;

    private final HttpServletRequest httpRequest;

    @Override
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody final UserRegistrationRequest request) {
        log.info("Received registration request for user with email = '{}'", request.email());
        emailTokenSender.sendEmailVerificationCode(request);
        log.info("Email verification token sent to the user with email = '{}'", request.email());
        return ResponseEntity.ok()
                .body(String.format("Email verification token sent to the user with email = %s%nIf You don't receive an email, please check your spam or may be the email address is incorrect", request.email()));
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
        return ResponseEntity.ok(authenticationResponse);
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthenticationResponse> refreshToken() {
        log.info("Received refresh token request for user");
        var authenticationToken = jwtAuthenticationProvider.get(httpRequest);
        UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationToken.getName());
        UserAuthenticationResponse authenticationResponse = userAuthenticationService.authenticate(userDetails, authenticationToken.getName());
        log.info("Refresh completed for user");
        return ResponseEntity.ok(authenticationResponse);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Received logout request");

        String token = jwtTokenFromAuthHeaderExtractor.extract(httpRequest);

        jwtBlacklistValidator.addToBlacklist(token);

        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody final ForgotPasswordRequest request) {
        log.info("Received forgot password request for user");
        UserDto userDto = singleUserProvider.getUserByEmail(request.email());
        UserRegistrationRequest requestVerification = new UserRegistrationRequest(userDto.getFirstName(), userDto.getLastName(),
                userDto.getEmail(), "");
        emailTokenSender.sendEmailVerificationCode(requestVerification);
        log.info("Send email with verification code for user");
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@RequestBody final ChangePasswordRequest request) {
        log.info("Received change password request for user");
        UserDto userDto = singleUserProvider.getUserByEmail(request.email());
        emailTokenConformer.confirmResetPasswordEmailByCode(
                new com.zufar.icedlatte.security.dto.ConfirmEmailRequest(request.code()));
        changeUserPasswordOperationPerformer.changeUserPassword(userDto.getId(), request.password());
        log.info("Password changed for user");
        return ResponseEntity.ok().build();
    }
}
