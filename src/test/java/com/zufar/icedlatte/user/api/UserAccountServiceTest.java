package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountService unit tests")
class UserAccountServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserAccountService service;

    @Test
    @DisplayName("deleteUser delegates to the repository with the provided user ID")
    void deleteUserDelegatesToRepository() {
        UUID userId = UUID.randomUUID();

        service.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }
}
