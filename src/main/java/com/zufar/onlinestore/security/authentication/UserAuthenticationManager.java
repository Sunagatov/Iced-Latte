package com.zufar.onlinestore.security.authentication;

import com.zufar.onlinestore.repository.UserDetailsRepository;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.security.dto.authentication.AuthenticationRequest;
import com.zufar.onlinestore.security.dto.authentication.AuthenticationResponse;
import com.zufar.onlinestore.security.dto.authentication.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationManager {
	private final UserDetailsRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;

	public AuthenticationResponse register(final RegisterRequest request) {
		log.info("Received registration request from {}.", request.getUsername());

		final UserDetails userDetails = User.builder()
				.username(request.getUsername())
				.password(passwordEncoder.encode(request.getPassword()))
				.roles("User")
				.build();

		repository.save(userDetails);

		final String jwtToken = jwtTokenProvider.generateToken(userDetails);

		log.info("Registration was successful for {}.", request.getUsername());


		return AuthenticationResponse.builder()
				.token(jwtToken)
				.build();
	}

	public AuthenticationResponse authenticate(final AuthenticationRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
		);

		var user = repository
				.findByUsername(request.getUsername())
				.orElseThrow();

		var jwtToken = jwtTokenProvider.generateToken(user);

		return AuthenticationResponse.builder()
				.token(jwtToken)
				.build();
	}

	public void logout(final HttpServletRequest request,
	                   final HttpServletResponse response) {
		SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
		securityContextLogoutHandler.logout(request, response, null);
	}
}