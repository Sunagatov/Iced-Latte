package com.zufar.onlinestore.security.authentication;

import com.zufar.onlinestore.security.converter.RegistrationDtoConverter;
import com.zufar.onlinestore.security.dto.authentication.RegistrationRequest;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.security.dto.authentication.AuthenticationRequest;
import com.zufar.onlinestore.security.dto.authentication.AuthenticationResponse;

import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationManager {

    private final UserApi userApi;
    private final UserDetailsService userDetailsService;
    private final RegistrationDtoConverter registrationDtoConverter;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;

	public AuthenticationResponse register(final RegistrationRequest request) {
		log.info("Received registration request from {}.", request.userName());
		final UserDto userDetails = registrationDtoConverter.toDto(request);
        userApi.saveUser(userDetails);
        User user = registrationDtoConverter.toUser(request);
        final String jwtToken = jwtTokenProvider.generateToken(user);
		log.info("Registration was successful for {}.", request.userName());
		return AuthenticationResponse.builder()
				.token(jwtToken)
				.build();
	}

	public AuthenticationResponse authenticate(final AuthenticationRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
		);
        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
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