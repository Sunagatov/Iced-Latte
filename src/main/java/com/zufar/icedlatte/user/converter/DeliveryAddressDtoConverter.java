package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeliveryAddressDtoConverter {

    @Mapping(target = "isDefault", source = "default")
    DeliveryAddressDto toDto(DeliveryAddressEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    DeliveryAddressEntity toEntity(DeliveryAddressRequest request);
}
