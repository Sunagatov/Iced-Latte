package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SingleUserProvider unit tests")
class SingleUserProviderTest {

    @Mock
    private UserRepository userCrudRepository;

    @InjectMocks
    private SingleUserProvider singleUserProvider;

    @Test
    @DisplayName("getUserEntityById returns entity directly")
    void getUserEntityByIdReturnsEntityDirectly() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = UserEntity.builder().id(userId).build();
        when(userCrudRepository.findById(userId)).thenReturn(java.util.Optional.of(entity));

        assertThat(singleUserProvider.getUserEntityById(userId)).isSameAs(entity);
        verify(userCrudRepository).findById(userId);
    }

    @Test
    @DisplayName("getUserEntityByEmail returns entity by email")
    void getUserEntityByEmailReturnsEntity() {
        UserEntity entity = UserEntity.builder().email("user@example.com").build();
        when(userCrudRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(entity));

        assertThat(singleUserProvider.getUserEntityByEmail("user@example.com")).isSameAs(entity);
        verify(userCrudRepository).findByEmail("user@example.com");
    }

    @Test
    @DisplayName("getUserEntityByEmail throws when user is missing")
    void getUserEntityByEmailThrowsWhenMissing() {
        when(userCrudRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> singleUserProvider.getUserEntityByEmail("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class);
        verify(userCrudRepository).findByEmail("missing@example.com");
    }
}
