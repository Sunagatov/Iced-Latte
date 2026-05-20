package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = AddressDtoConverter.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserDtoConverter {

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddressDto")
    @Mapping(target = "avatarLink", ignore = true)
    @Mapping(target = "oauthUser", source = "oauthUser")
    UserDto toDto(final UserEntity entity);

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddress")
    void updateEntity(@MappingTarget UserEntity entity, UpdateUserAccountRequest request);
}
