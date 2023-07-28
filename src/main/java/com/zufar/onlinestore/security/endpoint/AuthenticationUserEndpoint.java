package com.zufar.onlinestore.security.endpoint;

import com.zufar.onlinestore.security.authentication.UserAuthenticationManager;
import com.zufar.onlinestore.security.dto.authentication.AuthenticationRequest;
import com.zufar.onlinestore.security.dto.authentication.AuthenticationResponse;

import com.zufar.onlinestore.security.dto.authentication.RegistrationRequest;
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
@RequestMapping(value = "/api/auth")
@RequiredArgsConstructor
public class AuthenticationUserEndpoint {
	private final UserAuthenticationManager userAuthenticationManager;

	@PostMapping("/register")
	public ResponseEntity<AuthenticationResponse> register(@RequestBody @NotNull @Valid final RegistrationRequest request) {
		AuthenticationResponse authenticationResponse = userAuthenticationManager.register(request);
		return ResponseEntity
				.ok(authenticationResponse);
	}

	@PostMapping("/authenticate")
	public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @NotNull @Valid final AuthenticationRequest request) {
		AuthenticationResponse authenticationResponse = userAuthenticationManager.authenticate(request);
		return ResponseEntity
				.ok(authenticationResponse);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(final HttpServletRequest request,
	                                   final HttpServletResponse response) {
		userAuthenticationManager.logout(request, response);
		return ResponseEntity.ok()
				.build();
	}
}
