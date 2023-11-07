package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private UserRepository userRepository;
    private String userEmail = Instancio.of(String.class)
            .create();
    private UserEntity userDetails = Instancio.of(UserEntity.class)
            .create();

    @Test
    @DisplayName("test load user by username")
    void testLoadUserByUsername() {
        when(userRepository.findByEmail(userEmail))
                .thenReturn(Optional.ofNullable(userDetails));

        assertDoesNotThrow(() -> customUserDetailsService.loadUserByUsername(userEmail));

        verify(userRepository, times(1))
                .findByEmail(userEmail);
    }

    @Test
    @DisplayName("test load user by username throws UsernameNotFoundException")
    void testLoadUserByUsernameThrowsException() {
        when(userRepository.findByEmail(userEmail))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(userEmail));

        verify(userRepository, times(1))
                .findByEmail(userEmail);
    }
}