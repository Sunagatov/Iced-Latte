package com.zufar.icedlatte.security.signin.auth;

import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService unit tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserAuthenticationApi userAuthenticationApi;

    @InjectMocks
    private CustomUserDetailsService service;

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("normalizes email before API lookup")
        void normalizesEmailBeforeApiLookup() {
            UserAuthenticationSnapshot user = user();
            when(userAuthenticationApi.findUserAuthenticationByEmail("john.doe@example.com"))
                    .thenReturn(Optional.of(user));

            SecurityUserDetails result = (SecurityUserDetails) service.loadUserByUsername("  John.Doe@Example.com  ");

            assertThat(result.id()).isEqualTo(user.userId());
            assertThat(result.getUsername()).isEqualTo(user.email());
            assertThat(result.getPassword()).isEqualTo(user.password());
            assertThat(result.getAuthorities()).extracting("authority").containsExactly("USER");
            verify(userAuthenticationApi).findUserAuthenticationByEmail("john.doe@example.com");
            verifyNoMoreInteractions(userAuthenticationApi);
        }

        @Test
        @DisplayName("rejects blank email before repository lookup")
        void rejectsBlankEmailBeforeRepositoryLookup() {
            assertThatThrownBy(() -> service.loadUserByUsername("   "))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("Email cannot be empty");

            verifyNoMoreInteractions(userAuthenticationApi);
        }

        @Test
        @DisplayName("throws when normalized email is not found")
        void throwsWhenUserIsNotFound() {
            when(userAuthenticationApi.findUserAuthenticationByEmail("missing@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("Missing@Example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            verify(userAuthenticationApi).findUserAuthenticationByEmail("missing@example.com");
            verifyNoMoreInteractions(userAuthenticationApi);
        }
    }

    private static UserAuthenticationSnapshot user() {
        return new UserAuthenticationSnapshot(
                UUID.randomUUID(),
                "john.doe@example.com",
                "password123",
                List.of("USER"),
                true,
                true,
                true,
                true
        );
    }
}
