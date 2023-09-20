package com.zufar.onlinestore.security.endpoint;

import com.zufar.onlinestore.openapi.security.api.SecurityApi;
import com.zufar.onlinestore.security.api.UserSecurityManager;

import com.zufar.onlinestore.security.dto.UserRegistrationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.dto.UserRegistrationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping(value = UserSecurityEndpoint.USER_SECURITY_API_URL)
@RequiredArgsConstructor
public class UserSecurityEndpoint implements SecurityApi {

	public static final String USER_SECURITY_API_URL = "/api/v1/auth/";

	private final UserSecurityManager userSecurityManager;

	@Override
	@PostMapping("/register")
	public ResponseEntity<UserRegistrationResponse> register(@RequestBody @NotNull final UserRegistrationRequest request) {
		UserRegistrationResponse authenticationResponse = userSecurityManager.register(request);
		return new ResponseEntity<>(authenticationResponse, HttpStatus.CREATED);
	}

	@Override
	@PostMapping("/authenticate")
	public ResponseEntity<UserAuthenticationResponse> authenticate(@RequestBody @NotNull final UserAuthenticationRequest request) {
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
