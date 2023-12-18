package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AddressDtoConverter.class)
public abstract class UserDtoConverter {

    @Autowired
    private AddressDtoConverter addressDtoConverter;

    @Mapping(target = "address", source = "entity.address", qualifiedByName = "toAddressDto")
    public abstract UserDto toDto(final UserEntity entity);

    @Mapping(target = "address", source = "updateUserAccountRequest.address", qualifiedByName = "toAddress")
    public abstract UserEntity toEntity(final UpdateUserAccountRequest updateUserAccountRequest);

    @Named("toUserEntity")
    public UserEntity toEntity(final ShippingInfoDto shippingInfoDto) {
        if (shippingInfoDto == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(shippingInfoDto.getEmail());
        userEntity.setFirstName(shippingInfoDto.getFirstName());
        userEntity.setLastName(shippingInfoDto.getLastName());
        userEntity.setPhoneNumber(shippingInfoDto.getPhoneNumber());
        if (shippingInfoDto.getAddress() != null) {
            Address address = addressDtoConverter.toEntity(shippingInfoDto.getAddress());
            userEntity.setAddress(address);
        }
        return userEntity;
    }
}
