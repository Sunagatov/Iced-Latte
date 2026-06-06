package com.zufar.icedlatte.security.jwt.provider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;

@DisplayName("JwtAccountStatusValidator unit tests")
class JwtAccountStatusValidatorTest {

    @Test
    @DisplayName("allows active account")
    void allowsActiveAccount() {
        assertThatCode(() -> JwtAccountStatusValidator.requireActive(activeUser()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects disabled account")
    void rejectsDisabledAccount() {
        UserDetails user = User.withUserDetails(activeUser()).disabled(true).build();

        assertThatThrownBy(() -> JwtAccountStatusValidator.requireActive(user))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }

    @Test
    @DisplayName("rejects locked account")
    void rejectsLockedAccount() {
        UserDetails user =
                User.withUserDetails(activeUser()).accountLocked(true).build();

        assertThatThrownBy(() -> JwtAccountStatusValidator.requireActive(user))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }

    @Test
    @DisplayName("rejects expired account")
    void rejectsExpiredAccount() {
        UserDetails user =
                User.withUserDetails(activeUser()).accountExpired(true).build();

        assertThatThrownBy(() -> JwtAccountStatusValidator.requireActive(user))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }

    @Test
    @DisplayName("rejects expired credentials")
    void rejectsExpiredCredentials() {
        UserDetails user =
                User.withUserDetails(activeUser()).credentialsExpired(true).build();

        assertThatThrownBy(() -> JwtAccountStatusValidator.requireActive(user))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }

    private static UserDetails activeUser() {
        return User.withUsername("user@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
