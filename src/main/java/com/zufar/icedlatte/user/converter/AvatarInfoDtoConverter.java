package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.user.dto.AvatarInfoDto;
import com.zufar.icedlatte.user.entity.AvatarInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AvatarInfoDtoConverter {

    @Named("toAvatarInfoDto")
    AvatarInfoDto toDto(final AvatarInfo entity);

    @Named("toAvatarInfo")
    AvatarInfo toEntity(final AvatarInfoDto dto);
}
