package com.zufar.icedlatte.security.signin.auth;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAuthenticationApi userAuthenticationApi;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        if (!StringUtils.hasText(email)) {
            log.debug("auth.user_details.empty_email");
            throw new UsernameNotFoundException("Email cannot be empty");
        }
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userAuthenticationApi
                .findUserAuthenticationByEmail(normalizedEmail)
                .map(SecurityUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
