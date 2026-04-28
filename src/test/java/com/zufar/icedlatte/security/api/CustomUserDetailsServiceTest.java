package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService unit tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("normalizes email before repository lookup")
        void normalizesEmailBeforeRepositoryLookup() {
            UserEntity user = user();
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

            UserEntity result = (UserEntity) service.loadUserByUsername("  John.Doe@Example.com  ");

            assertThat(result).isSameAs(user);
            verify(userRepository).findByEmail("john.doe@example.com");
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("rejects blank email before repository lookup")
        void rejectsBlankEmailBeforeRepositoryLookup() {
            assertThatThrownBy(() -> service.loadUserByUsername("   "))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("Email cannot be empty");

            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("throws when normalized email is not found")
        void throwsWhenUserIsNotFound() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("Missing@Example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findByEmail("missing@example.com");
            verifyNoMoreInteractions(userRepository);
        }
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("john.doe@example.com");
        user.setPassword("password123");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        return user;
    }
}
