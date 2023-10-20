package com.zufar.onlinestore.user.stub;

import com.zufar.onlinestore.openapi.dto.AddressDto;
import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.entity.Address;
import com.zufar.onlinestore.user.entity.UserEntity;

import java.util.UUID;

public class UserDtoTestUtil {

    public static UserEntity createUserEntity() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        Address address = Address.builder()
                .addressId(UUID.randomUUID())
                .line("123 Main St")
                .city("Sample City")
                .country("Sample Country")
                .build();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("johndoe@example.com");
        entity.setPassword("password123");
        entity.setStripeCustomerToken("tok_123456789");
        entity.setAddress(address);
        return entity;
    }

    public static UserDto createUserDto() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        AddressDto addressDto = new AddressDto();
        addressDto.setLine("456 Elm St");
        addressDto.setCity("Test City");
        addressDto.setCountry("Test Country");

        UserDto dto = new UserDto();
        dto.setId(userId);
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("johndoe@example.com");
        dto.setPassword("password123");
        dto.setStripeCustomerToken("tok_123456789");
        dto.setAddress(addressDto);
        return dto;
    }
}
