package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.Authority;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.entity.UserGrantedAuthority;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.Set;

@Mapper(componentModel = "spring", uses = AddressDtoConverter.class)
public interface UserDtoConverter {

    AddressDtoConverter addressConverter = Mappers.getMapper(AddressDtoConverter.class);

    @Mapping(target = "address", source = "entity.address", qualifiedByName = "toAddressDto")
    UserDto toDto(final UserEntity entity);

    @Mapping(target = "accountNonExpired", constant = "true")
    @Mapping(target = "accountNonLocked", constant = "true")
    @Mapping(target = "credentialsNonExpired", constant = "true")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "address", source = "dto.address", qualifiedByName = "toAddress")
    @Mapping(target = "authorities", expression = "java(createAuthorities(dto))")
    UserEntity toEntity(final UserDto dto);

    default Set<UserGrantedAuthority> createAuthorities(UserDto dto) {
        UserEntity userEntity = UserEntity.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email())
                .username(dto.username())
                .password(dto.password())
                .address(addressConverter.toEntity((dto.address())))
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

        return authorities;
    }
}