package com.zufar.icedlatte.security.jwt.provider;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;

@Component
public class JwtAccountStatusValidator {

    public void requireActive(UserDetails userDetails) {
        if (!userDetails.isEnabled()
                || !userDetails.isAccountNonLocked()
                || !userDetails.isAccountNonExpired()
                || !userDetails.isCredentialsNonExpired()) {
            throw new JwtTokenBlacklistedException("User account is not active");
        }
    }
}
