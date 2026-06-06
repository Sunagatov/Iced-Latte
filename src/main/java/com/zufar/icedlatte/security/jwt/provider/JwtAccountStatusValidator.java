package com.zufar.icedlatte.security.jwt.provider;

import org.springframework.security.core.userdetails.UserDetails;

import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtAccountStatusValidator {

    public static void requireActive(UserDetails userDetails) {
        if (!userDetails.isEnabled()
                || !userDetails.isAccountNonLocked()
                || !userDetails.isAccountNonExpired()
                || !userDetails.isCredentialsNonExpired()) {
            throw new JwtTokenBlacklistedException("User account is not active");
        }
    }
}
