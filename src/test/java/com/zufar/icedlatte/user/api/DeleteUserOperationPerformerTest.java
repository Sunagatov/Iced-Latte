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
@DisplayName("DeleteUserOperationPerformer unit tests")
class DeleteUserOperationPerformerTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private DeleteUserOperationPerformer performer;

    @Test
    @DisplayName("deleteUser delegates to userRepository.deleteById")
    void deleteUser_callsRepository() {
        UUID userId = UUID.randomUUID();

        performer.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }
}
