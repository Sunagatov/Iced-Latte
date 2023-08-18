package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AddressDtoConverter.class)
public interface UserDtoConverter {

    @Mapping(target = "address", source = "entity.address", qualifiedByName = "toAddressDto")
    UserDto toDto(final UserEntity entity);

    @Mapping(target = "accountNonExpired", constant = "true")
    @Mapping(target = "accountNonLocked", constant = "true")
    @Mapping(target = "credentialsNonExpired", constant = "true")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "address", source = "dto.address", qualifiedByName = "toAddress")
    @Mapping(target = "authorities",
            expression = "java(java.util.Collections.singleton(com.zufar.onlinestore.user.entity.UserGrantedAuthority" +
                    ".builder().authority(com.zufar.onlinestore.user.entity.Authority.USER)" +
                    ".user(com.zufar.onlinestore.user.entity.UserEntity" +
                    ".builder().firstName(dto.firstName()).lastName(dto.lastName()).email(dto.email())" +
                    ".username(dto.username()).password(dto.password())" +
                    ".address(new AddressDtoConverterImpl().toEntity((dto.address()))).accountNonExpired(true)" +
                    ".accountNonLocked(true).credentialsNonExpired(true).enabled(true).build()).build()))")
    UserEntity toEntity(final UserDto dto);
}