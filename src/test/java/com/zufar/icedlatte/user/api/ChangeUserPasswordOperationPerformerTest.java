package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.InvalidOldPasswordException;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeUserPasswordOperationPerformer unit tests")
class ChangeUserPasswordOperationPerformerTest {

    @Mock
    private SingleUserProvider singleUserProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private ChangeUserPasswordOperationPerformer performer;

    @Test
    @DisplayName("Changes password when old password matches")
    void changeUserPassword_validOldPassword_updatesPassword() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");
        user.setPassword("encoded_old");

        ChangeUserPasswordRequest request = new ChangeUserPasswordRequest();
        request.setOldPassword("old_plain");
        request.setNewPassword("new_plain");

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(singleUserProvider.getUserEntityById(userId)).thenReturn(user);
        when(passwordEncoder.matches("old_plain", "encoded_old")).thenReturn(true);
        when(passwordEncoder.encode("new_plain")).thenReturn("encoded_new");

        performer.changeUserPassword(request);

        verify(userRepository).changeUserPassword("encoded_new", userId);
    }

    @Test
    @DisplayName("Throws InvalidOldPasswordException when old password does not match")
    void changeUserPassword_wrongOldPassword_throwsInvalidOldPasswordException() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");
        user.setPassword("encoded_old");

        ChangeUserPasswordRequest request = new ChangeUserPasswordRequest();
        request.setOldPassword("wrong_plain");
        request.setNewPassword("new_plain");

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(singleUserProvider.getUserEntityById(userId)).thenReturn(user);
        when(passwordEncoder.matches("wrong_plain", "encoded_old")).thenReturn(false);

        assertThatThrownBy(() -> performer.changeUserPassword(request))
                .isInstanceOf(InvalidOldPasswordException.class);

        verify(userRepository, never()).changeUserPassword(any(), any());
    }

    @Test
    @DisplayName("Direct UUID+password overload encodes and saves without security context")
    void changeUserPassword_directOverload_encodesAndSaves() {
        UUID userId = UUID.randomUUID();
        when(passwordEncoder.encode("new_plain")).thenReturn("encoded_new");

        performer.changeUserPassword(userId, "new_plain");

        verify(userRepository).changeUserPassword("encoded_new", userId);
        verifyNoInteractions(securityPrincipalProvider, singleUserProvider);
    }
}
