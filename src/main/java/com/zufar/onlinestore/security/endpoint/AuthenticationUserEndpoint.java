package com.zufar.onlinestore.security.endpoint;

import com.zufar.onlinestore.security.api.UserSecurityManager;

import com.zufar.onlinestore.security.dto.registration.UserRegistrationRequest;
import com.zufar.onlinestore.security.dto.authentication.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.authentication.UserAuthenticationResponse;
import com.zufar.onlinestore.security.dto.registration.UserRegistrationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping(value = AuthenticationUserEndpoint.USER_AUTH_API_URL)
@RequiredArgsConstructor
public class AuthenticationUserEndpoint {

    public static final String USER_AUTH_API_URL = "/api/v1/auth/";

    private final UserSecurityManager userSecurityManager;

	@PostMapping("/register")
	public ResponseEntity<UserRegistrationResponse> register(@RequestBody @NotNull @Valid final UserRegistrationRequest request) {
        UserRegistrationResponse authenticationResponse = userSecurityManager.register(request);
		return ResponseEntity
				.ok(authenticationResponse);
	}

	@PostMapping("/authenticate")
	public ResponseEntity<UserAuthenticationResponse> authenticate(@RequestBody @NotNull @Valid final UserAuthenticationRequest request) {
        UserAuthenticationResponse authenticationResponse = userSecurityManager.authenticate(request);
		return ResponseEntity
				.ok(authenticationResponse);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(final HttpServletRequest request,
	                                   final HttpServletResponse response) {
		userSecurityManager.logout(request, response);
		return ResponseEntity.ok()
				.build();
	}
}
