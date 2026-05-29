package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountRegistrationService unit tests")
class UserAccountRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAccountRegistrationService service;

    @Test
    @DisplayName("existsByEmail delegates to repository")
    void existsByEmailDelegatesToRepository() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThat(service.existsByEmail("user@example.com")).isTrue();
        verify(userRepository).existsByEmail("user@example.com");
    }

    @Test
    @DisplayName("registerPasswordUser persists default enabled user and returns authentication snapshot")
    void registerPasswordUserPersistsDefaultEnabledUserAndReturnsSnapshot() {
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(UUID.randomUUID());
                    return user;
                });

        var snapshot = service.registerPasswordUser("Alice", "Example", "alice@example.com", "encoded-password");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("Alice");
        assertThat(savedUser.getLastName()).isEqualTo("Example");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.isOauthUser()).isFalse();
        assertThat(savedUser.isAccountNonExpired()).isTrue();
        assertThat(savedUser.isAccountNonLocked()).isTrue();
        assertThat(savedUser.isCredentialsNonExpired()).isTrue();
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getAuthorities())
                .singleElement()
                .extracting(UserGrantedAuthority::getAuthority)
                .isEqualTo(Authority.USER.name());

        assertThat(snapshot.userId()).isEqualTo(savedUser.getId());
        assertThat(snapshot.email()).isEqualTo("alice@example.com");
        assertThat(snapshot.passwordHash()).isEqualTo("encoded-password");
        assertThat(snapshot.authorities()).containsExactly(Authority.USER.name());
    }

    @Test
    @DisplayName("registerOAuthUser persists oauth user")
    void registerOAuthUserPersistsOauthUser() {
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(UUID.randomUUID());
                    return user;
                });

        var snapshot = service.registerOAuthUser("OAuth", "User", "oauth@example.com", "encoded-password");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();

        assertThat(savedUser.isOauthUser()).isTrue();
        assertThat(savedUser.getEmail()).isEqualTo("oauth@example.com");
        assertThat(savedUser.getAuthorities())
                .singleElement()
                .extracting(UserGrantedAuthority::getAuthority)
                .isEqualTo(Authority.USER.name());
        assertThat(snapshot.email()).isEqualTo("oauth@example.com");
    }
}
