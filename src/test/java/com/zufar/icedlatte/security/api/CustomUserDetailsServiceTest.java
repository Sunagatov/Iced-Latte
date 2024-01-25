package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserRepository userRepository;

    private UserEntity userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new UserEntity();
        userDetails.setId(UUID.randomUUID());
        userDetails.setFirstName("John");
        userDetails.setLastName("Doe");
        userDetails.setEmail("john.doe@example.com");
        userDetails.setPassword("password123");
        userDetails.setAccountNonExpired(true);
        userDetails.setAccountNonLocked(true);
        userDetails.setCredentialsNonExpired(true);
        userDetails.setEnabled(true);
    }

    @Test
    @DisplayName("Should Load User By Username Successfully")
    void givenUserExistsInRepositoryWhenLoadUserByUsernameThenReturnUserDetails() {
        String userEmail = userDetails.getEmail();
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(userDetails));

        UserDetails result = customUserDetailsService.loadUserByUsername(userEmail);

        assertAll("User details should match",
                () -> assertEquals(userDetails.getEmail(), result.getUsername(), "Email should match"),
                () -> assertEquals(userDetails.isAccountNonExpired(), result.isAccountNonExpired(), "Account non-expired status should match"),
                () -> assertEquals(userDetails.isAccountNonLocked(), result.isAccountNonLocked(), "Account non-locked status should match"),
                () -> assertEquals(userDetails.isCredentialsNonExpired(), result.isCredentialsNonExpired(), "Credentials non-expired status should match"),
                () -> assertEquals(userDetails.isEnabled(), result.isEnabled(), "Enabled status should match"),
                () -> assertEquals(userDetails.getAuthorities(), result.getAuthorities(), "Authorities should match"),
                () -> assertEquals(userDetails.getPassword(), result.getPassword(), "Password should match"),
                () -> assertEquals(userDetails.getFirstName(), ((UserEntity) result).getFirstName(), "First name should match"),
                () -> assertEquals(userDetails.getLastName(), ((UserEntity) result).getLastName(), "Last name should match")
        );

        verify(userRepository, times(1)).findByEmail(userEmail);
    }

    @Test
    @DisplayName("Should Throw UsernameNotFoundException When User Not Found")
    void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
        String userEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        UsernameNotFoundException thrown = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(userEmail),
                "Expected UsernameNotFoundException to be thrown"
        );

        assertEquals("Invalid credentials for user's account with email = '" + userEmail + "'", thrown.getMessage());
        verify(userRepository, times(1)).findByEmail(userEmail);
    }
}
