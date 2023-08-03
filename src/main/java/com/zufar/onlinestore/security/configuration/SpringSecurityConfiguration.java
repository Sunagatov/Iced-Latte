package com.zufar.onlinestore.security.configuration;

import com.zufar.onlinestore.security.endpoint.AuthenticationUserEndpoint;
import com.zufar.onlinestore.security.jwt.filter.JwtAuthenticationFilter;

import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

    private static final String API_AUTH_URL_PREFIX = AuthenticationUserEndpoint.USER_AUTH_API_URL + "**";
    private static final String API_DOCS_URL_PREFIX = "/api/docs/**";

    public static final String ACTUATOR_ENDPOINTS_URL_PREFIX = "/actuator/**";
    public static final String WEBHOOK_PAYMENT_EVENT_ENDPOINT = "/api/v1/payment/event";

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity,
                                                   final JwtAuthenticationFilter jwtTokenFilter) throws Exception {
        return httpSecurity
                .csrf().disable()
                .authorizeHttpRequests()
                .requestMatchers(API_AUTH_URL_PREFIX).permitAll()
                .requestMatchers(WEBHOOK_PAYMENT_EVENT_ENDPOINT).permitAll()
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
    public UserDetailsService userDetailsService(final UserRepository userRepository,
                                                 final PasswordEncoder passwordEncoder) {
        return username -> {
            UserEntity user = userRepository.findUserByUsername(username);
            if (user == null) {
                log.warn("Failed to get the user with the username = {}.", username);
                throw new UsernameNotFoundException(username);
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return user;
        };

    }

    @Bean
    public AuthenticationProvider authenticationProvider(final UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(final HttpSecurity httpSecurity,
                                                       final PasswordEncoder passwordEncoder,
                                                       final UserDetailsService userDetailService) throws Exception {
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
