package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;

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
    @DisplayName("getUserById returns lookup snapshot")
    void getUserByIdReturnsLookupSnapshot() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = UserEntity.builder()
                .id(userId)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .build();
        when(userCrudRepository.findById(userId)).thenReturn(java.util.Optional.of(entity));

        var snapshot = singleUserProvider.getUserById(userId);

        assertThat(snapshot.id()).isEqualTo(userId);
        assertThat(snapshot.firstName()).isEqualTo("Ada");
        assertThat(snapshot.lastName()).isEqualTo("Lovelace");
        assertThat(snapshot.email()).isEqualTo("ada@example.com");
        verify(userCrudRepository).findById(userId);
    }

    @Test
    @DisplayName("getUsersByIds returns lookup snapshots in a single repository call")
    void getUsersByIdsReturnsLookupSnapshots() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = UserEntity.builder()
                .id(userId)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .build();
        when(userCrudRepository.findAllById(Set.of(userId))).thenReturn(List.of(entity));

        var snapshots = singleUserProvider.getUsersByIds(Set.of(userId));

        assertThat(snapshots).extracting("id").containsExactly(userId);
        verify(userCrudRepository).findAllById(Set.of(userId));
    }

    @Test
    @DisplayName("getUserEntityByEmail normalizes email before lookup")
    void getUserEntityByEmailNormalizesEmailBeforeLookup() {
        UserEntity entity = UserEntity.builder().email("user@example.com").build();
        when(userCrudRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(entity));

        assertThat(singleUserProvider.getUserEntityByEmail(" User@Example.COM "))
                .isSameAs(entity);
        verify(userCrudRepository).findByEmail("user@example.com");
    }

    @Test
    @DisplayName("getUserByEmail returns lookup snapshot")
    void getUserByEmailReturnsLookupSnapshot() {
        UserEntity entity = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Grace")
                .lastName("Hopper")
                .email("user@example.com")
                .build();
        when(userCrudRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(entity));

        var snapshot = singleUserProvider.getUserByEmail("user@example.com");

        assertThat(snapshot.id()).isEqualTo(entity.getId());
        assertThat(snapshot.firstName()).isEqualTo("Grace");
        assertThat(snapshot.lastName()).isEqualTo("Hopper");
        assertThat(snapshot.email()).isEqualTo("user@example.com");
        verify(userCrudRepository).findByEmail("user@example.com");
    }

    @Test
    @DisplayName("findUserByEmail returns lookup snapshot when present")
    void findUserByEmailReturnsLookupSnapshotWhenPresent() {
        UserEntity entity = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Grace")
                .lastName("Hopper")
                .email("user@example.com")
                .build();
        when(userCrudRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(entity));

        var snapshot = singleUserProvider.findUserByEmail(" User@Example.COM ");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().id()).isEqualTo(entity.getId());
        verify(userCrudRepository).findByEmail("user@example.com");
    }

    @Test
    @DisplayName("findUserByEmail returns empty when missing")
    void findUserByEmailReturnsEmptyWhenMissing() {
        when(userCrudRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        assertThat(singleUserProvider.findUserByEmail("missing@example.com")).isEmpty();

        verify(userCrudRepository).findByEmail("missing@example.com");
    }

    @Test
    @DisplayName("findUserAuthenticationByEmail returns authentication snapshot")
    void findUserAuthenticationByEmailReturnsSnapshot() {
        UserEntity entity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("encoded")
                .authorities(java.util.Set.of(
                        UserGrantedAuthority.builder().authority(Authority.USER).build()))
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
        when(userCrudRepository.findByEmailWithAuthorities("user@example.com"))
                .thenReturn(java.util.Optional.of(entity));

        var snapshot = singleUserProvider.findUserAuthenticationByEmail(" User@Example.COM ");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().userId()).isEqualTo(entity.getId());
        assertThat(snapshot.orElseThrow().email()).isEqualTo(entity.getEmail());
        assertThat(snapshot.orElseThrow().encodedPassword()).isEqualTo(entity.getPassword());
        assertThat(snapshot.orElseThrow().authorities()).containsExactly("USER");
        assertThat(snapshot.orElseThrow().accountNonExpired()).isTrue();
        assertThat(snapshot.orElseThrow().accountNonLocked()).isTrue();
        assertThat(snapshot.orElseThrow().credentialsNonExpired()).isTrue();
        assertThat(snapshot.orElseThrow().enabled()).isTrue();
        verify(userCrudRepository).findByEmailWithAuthorities("user@example.com");
    }

    @Test
    @DisplayName("findUserAuthenticationByEmail returns empty when user is missing")
    void findUserAuthenticationByEmailReturnsEmptyWhenMissing() {
        when(userCrudRepository.findByEmailWithAuthorities("missing@example.com"))
                .thenReturn(java.util.Optional.empty());

        assertThat(singleUserProvider.findUserAuthenticationByEmail("missing@example.com"))
                .isEmpty();
        verify(userCrudRepository).findByEmailWithAuthorities("missing@example.com");
    }

    @Test
    @DisplayName("findUserAuthenticationById returns authentication snapshot")
    void findUserAuthenticationByIdReturnsSnapshot() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = UserEntity.builder()
                .id(userId)
                .email("user@example.com")
                .password("encoded")
                .authorities(java.util.Set.of(
                        UserGrantedAuthority.builder().authority(Authority.USER).build()))
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
        when(userCrudRepository.findByIdWithAuthorities(userId)).thenReturn(java.util.Optional.of(entity));

        var snapshot = singleUserProvider.findUserAuthenticationById(userId);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().userId()).isEqualTo(userId);
        assertThat(snapshot.orElseThrow().email()).isEqualTo("user@example.com");
        verify(userCrudRepository).findByIdWithAuthorities(userId);
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
