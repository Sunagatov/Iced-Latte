package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserDtoConverterTest.Config.class)
class UserDtoConverterTest {

    @Configuration
    static class Config {
        @Bean UserDtoConverter userDtoConverter() { return Mappers.getMapper(UserDtoConverter.class); }
        @Bean AddressDtoConverter addressDtoConverter() { return Mappers.getMapper(AddressDtoConverter.class); }
    }

    @Autowired
    private UserDtoConverter userDtoConverter;

    @Test
    @DisplayName("toDto should convert UserEntity to UserDto with complete user information")
    void toDtoShouldConvertUserEntityToUserDtoWithCompleteUserInformation() {
        UserEntity entity = UserDtoTestStub.createUserEntity();
        UserDto dto = userDtoConverter.toDto(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getFirstName(), dto.getFirstName());
        assertEquals(entity.getLastName(), dto.getLastName());
        assertEquals(entity.getBirthDate(), dto.getBirthDate());
        assertEquals(entity.getPhoneNumber(), dto.getPhoneNumber());
        assertEquals(entity.getEmail(), dto.getEmail());
        assertEquals(entity.getStripeCustomerToken(), dto.getStripeCustomerToken());
        assertNotNull(dto.getAddress());
        assertEquals(entity.getAddress().getLine(), dto.getAddress().getLine());
        assertEquals(entity.getAddress().getCity(), dto.getAddress().getCity());
        assertEquals(entity.getAddress().getCountry(), dto.getAddress().getCountry());
    }
}