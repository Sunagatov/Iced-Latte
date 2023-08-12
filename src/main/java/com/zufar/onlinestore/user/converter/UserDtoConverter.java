package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.user.dto.AddressDto;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.Address;
import com.zufar.onlinestore.user.entity.Authority;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.entity.UserGrantedAuthority;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserDtoConverter {
    private final AddressDtoConverter addressDtoConverter;

    public UserDto toDto(final UserEntity entity) {
        AddressDto addressDto = null;
        if (entity.getAddress() != null) {
            addressDto = addressDtoConverter.toDto(entity.getAddress());
        }
        return new UserDto(
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPassword(),
                addressDto
        );
    }

    public UserEntity toEntity(final UserDto dto) {
        Address address = null;
        if (dto.address() != null) {
            address = addressDtoConverter.toEntity(dto.address());
        }

        UserEntity userEntity = UserEntity.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email())
                .username(dto.username())
                .password(dto.password())
                .address(address)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();

        Set<UserGrantedAuthority> authorities = Collections
                .singleton(UserGrantedAuthority.builder()
                        .authority(Authority.USER)
                        .user(userEntity)
                        .build());

        userEntity.setAuthorities(authorities);

        return userEntity;
    }
}
