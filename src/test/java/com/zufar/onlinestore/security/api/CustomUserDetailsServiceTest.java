package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private String userEmail = "TestEmail";
    private UserEntity userDetails = Instancio.create(UserEntity.class);

    @Test
    @DisplayName("Should_LoadUserByUsername_Successfully")
    void givenUserExistsInRepositoryWhenLoadUserByUsernameThenReturnUserDetails() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.ofNullable(userDetails));

        UserDetails result = customUserDetailsService.loadUserByUsername(userEmail);

        assertEquals(result, userDetails);
        verify(userRepository, times(1)).findByEmail(userEmail);
    }

    @Test
    @DisplayName("Should_ThrowUsernameNotFoundException_WhenUserNotFound")
    void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername(userEmail));

        verify(userRepository, times(1)).findByEmail(userEmail);
    }
}