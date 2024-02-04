package com.zufar.icedlatte.security.converter;

import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.user.converter.AddressDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationDtoConverter {

    private final AddressDtoConverter addressDtoConverter;

    public UserEntity toEntity(final UserRegistrationRequest userRegistrationRequest){
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName(userRegistrationRequest.firstName());
        userEntity.setLastName(userRegistrationRequest.lastName());
        userEntity.setBirthDate(userRegistrationRequest.birthDate());
        userEntity.setPhoneNumber(userRegistrationRequest.phoneNumber());
        userEntity.setEmail(userRegistrationRequest.email());
        userEntity.setPassword(userRegistrationRequest.password());
        userEntity.setAddress(addressDtoConverter.toEntity(userRegistrationRequest.addressDto()));
        return userEntity;
    }
}
