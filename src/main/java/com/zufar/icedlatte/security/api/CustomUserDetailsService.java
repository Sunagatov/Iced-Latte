package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (!StringUtils.hasText(email)) {
            log.warn("Attempted to load user with empty or null email");
            throw new UsernameNotFoundException("Email cannot be empty");
        }
        String normalizedEmail = email.toLowerCase(Locale.ROOT).trim();
        log.debug("Loading user details for email: {}", normalizedEmail);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", normalizedEmail);
                    return new InvalidCredentialsException();
                });
    }
}
