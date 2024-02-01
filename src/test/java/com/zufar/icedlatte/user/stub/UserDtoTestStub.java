package com.zufar.icedlatte.user.stub;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.UserEntity;

import java.time.LocalDate;
import java.util.UUID;

public class UserDtoTestStub {

    public static UserEntity createUserEntity() {
        Address address = AddressDtoTestStub.createAddressEntity();

        UserEntity entity = new UserEntity();
        entity.setId(UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3"));
        entity.setBirthDate(LocalDate.of(1997, 3, 12));
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("johndoe@example.com");
        entity.setPassword("password123");
        entity.setStripeCustomerToken("tok_123456789");
        entity.setPhoneNumber("79114446655");

        return entity;
    }


    public static UserDto createUserDto() {
        AddressDto addressDto = AddressDtoTestStub.createAddressDto();

        UserDto dto = new UserDto();
        dto.setId(UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3"));
        dto.setBirthDate(LocalDate.of(1997, 3, 12));
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("johndoe@example.com");
        dto.setStripeCustomerToken("tok_123456789");
        dto.setPhoneNumber("79114446655");

        return dto;
    }
}
