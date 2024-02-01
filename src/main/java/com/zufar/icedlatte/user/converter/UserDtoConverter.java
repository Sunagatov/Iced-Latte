package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;

@Service
public class UserDtoConverter {
    private AddressDtoConverter converter;
    public UserDto toDto(final UserEntity entity){
        UserDto userDto = new UserDto(entity.getFirstName(), entity.getLastName(), entity.getEmail());
        userDto.setId(entity.getId());
        userDto.setBirthDate(entity.getBirthDate());
        userDto.setPhoneNumber(entity.getPhoneNumber());
        userDto.setAddress(converter.toDto(entity.getAddress()));
        userDto.setStripeCustomerToken(entity.getStripeCustomerToken());
        return userDto;
    }

    public UserEntity toEntity(final UpdateUserAccountRequest updateUserAccountRequest){
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName(updateUserAccountRequest.getFirstName());
        userEntity.setLastName(updateUserAccountRequest.getLastName());
        userEntity.setAddress(converter.toEntity(updateUserAccountRequest.getAddress()));
        userEntity.setBirthDate(updateUserAccountRequest.getBirthDate());
        userEntity.setPhoneNumber(updateUserAccountRequest.getPhoneNumber());
        return userEntity;
    }
}
