package com.zufar.onlinestore.security.configuration;

import com.zufar.onlinestore.security.authentication.UserDetailsServiceImpl;
import com.zufar.onlinestore.security.jwt.filter.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfiguration {
	private static final String API_AUTH_URL_PREFIX = "/api/auth/**";
	private static final String API_DOCS_URL_PREFIX = "/api/docs/**";
	public static final String ACTUATOR_ENDPOINTS_URL_PREFIX = "/actuator/**";

	private final UserDetailsServiceImpl userDetailsService;
	private final JwtAuthenticationFilter jwtTokenFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
				.csrf().disable()
				.authorizeHttpRequests()
				.requestMatchers(API_AUTH_URL_PREFIX).permitAll()
				.requestMatchers(API_DOCS_URL_PREFIX).permitAll()
				.requestMatchers(ACTUATOR_ENDPOINTS_URL_PREFIX).permitAll()
				.anyRequest().authenticated()
				.and()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		return authenticationProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(final HttpSecurity httpSecurity,
	                                                   final PasswordEncoder passwordEncoder,
	                                                   final UserDetailsServiceImpl userDetailService) throws Exception {
		return httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
				.userDetailsService(userDetailService)
				.passwordEncoder(passwordEncoder)
				.and()
				.build();
	}
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
