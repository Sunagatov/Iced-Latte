package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultUserEntityValuesSetterTest {

    @Test
    void setDefaultValues_ShouldSetAllNeededFields() {
        DefaultUserEntityValuesSetter defaultUserEntityValuesSetter = new DefaultUserEntityValuesSetter();
        UserEntity userEntity = new UserEntity();
        UserGrantedAuthority defaultAuthority = UserGrantedAuthority.builder().authority(Authority.USER).build();
        Set<UserGrantedAuthority> expectedAuthorities = Set.of(defaultAuthority);

        defaultUserEntityValuesSetter.setDefaultValues(userEntity);

        assertEquals(expectedAuthorities.size(), userEntity.getAuthorities().size());
        assertThat(userEntity.getAuthorities()).isEqualTo(expectedAuthorities);
        assertTrue(userEntity.isAccountNonExpired());
        assertTrue(userEntity.isAccountNonLocked());
        assertTrue(userEntity.isCredentialsNonExpired());
        assertTrue(userEntity.isEnabled());
    }
}
