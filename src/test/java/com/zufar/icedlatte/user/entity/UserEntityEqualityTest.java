package com.zufar.icedlatte.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User entity equality")
class UserEntityEqualityTest {

    @Test
    @DisplayName("transient entities without ids are not equal")
    void transientEntitiesWithoutIdsAreNotEqual() {
        assertThat(new Address()).isNotEqualTo(new Address());
        assertThat(new DeliveryAddressEntity()).isNotEqualTo(new DeliveryAddressEntity());
        assertThat(new UserEntity()).isNotEqualTo(new UserEntity());
        assertThat(new UserGrantedAuthority()).isNotEqualTo(new UserGrantedAuthority());
    }

    @Test
    @DisplayName("entities with the same persisted id are equal")
    void entitiesWithSamePersistedIdAreEqual() {
        UUID id = UUID.randomUUID();

        assertThat(Address.builder().addressId(id).build())
                .isEqualTo(Address.builder().addressId(id).build());
        assertThat(DeliveryAddressEntity.builder().id(id).build())
                .isEqualTo(DeliveryAddressEntity.builder().id(id).build());
        assertThat(UserEntity.builder().id(id).build())
                .isEqualTo(UserEntity.builder().id(id).build());
        assertThat(UserGrantedAuthority.builder().userAuthorityId(id).build())
                .isEqualTo(UserGrantedAuthority.builder().userAuthorityId(id).build());
    }

    @Test
    @DisplayName("addAuthority ignores duplicate authority names for the same user")
    void addAuthorityIgnoresDuplicateAuthorityNamesForSameUser() {
        UserEntity user = new UserEntity();

        user.addAuthority(
                UserGrantedAuthority.builder().authority(Authority.USER).build());
        user.addAuthority(
                UserGrantedAuthority.builder().authority(Authority.USER).build());

        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getUser()).isSameAs(user);
    }
}
