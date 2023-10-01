package com.zufar.onlinestore.security.configuration;

import com.zufar.onlinestore.security.endpoint.UserSecurityEndpoint;
import com.zufar.onlinestore.security.jwt.filter.JwtAuthenticationFilter;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

    private static final String API_AUTH_URL_PREFIX = UserSecurityEndpoint.USER_SECURITY_API_URL + "**";
    private static final String API_DOCS_URL_PREFIX = "/api/docs/**";
    private static final String ACTUATOR_ENDPOINTS_URL_PREFIX = "/actuator/**";
    private static final String WEBHOOK_PAYMENT_EVENT_URL_PREFIX = "/api/v1/payment/event";
    private static final String PRODUCTS_API_URL_PREFIX = "/api/v1/products/**";

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity,
                                                   final JwtAuthenticationFilter jwtTokenFilter) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(API_AUTH_URL_PREFIX).permitAll()
                                .requestMatchers(WEBHOOK_PAYMENT_EVENT_URL_PREFIX).permitAll()
                                .requestMatchers(PRODUCTS_API_URL_PREFIX).permitAll()
                                .requestMatchers(API_DOCS_URL_PREFIX).permitAll()
                                .requestMatchers(ACTUATOR_ENDPOINTS_URL_PREFIX).permitAll()
                                .anyRequest().authenticated()
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(final UserRepository userRepository,
                                                 final PasswordEncoder passwordEncoder) {
        return email -> {
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("Failed to get the user with the username = {}.", email);
                        return new BadCredentialsException("Bad credentials");
                    });
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
        AuthenticationManagerBuilder authenticationManagerBuilder = httpSecurity
                .getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .userDetailsService(userDetailService)
                .passwordEncoder(passwordEncoder);

        return authenticationManagerBuilder
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
