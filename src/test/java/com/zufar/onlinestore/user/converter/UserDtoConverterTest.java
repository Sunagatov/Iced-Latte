package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = UserDtoConverterTest.Config.class)
class UserDtoConverterTest {

    @Autowired
    private UserDtoConverter userDtoConverter;

    @Configuration
    static class Config {

        @Bean
        UserDtoConverter userDtoConverter() {
            return Mappers.getMapper(UserDtoConverter.class);
        }

        @Bean
        AddressDtoConverter addressDtoConverter() {
            return Mappers.getMapper(AddressDtoConverter.class);
        }
    }

    @Test
    @DisplayName("toDto should convert UserEntity to UserDto with complete user information")
    void toDtoShouldConvertUserEntityToUserDtoWithCompleteUserInformation() {
        UserEntity entity = UserDtoTestStub.createUserEntity();
        UserDto dto = userDtoConverter.toDto(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getFirstName(), dto.getFirstName());
        assertEquals(entity.getLastName(), dto.getLastName());
        assertEquals(entity.getEmail(), dto.getEmail());
        assertEquals(entity.getPassword(), dto.getPassword());
        assertEquals(entity.getStripeCustomerToken(), dto.getStripeCustomerToken());
        assertEquals(entity.getAddress().getLine(), dto.getAddress().getLine());
        assertEquals(entity.getAddress().getCity(), dto.getAddress().getCity());
        assertEquals(entity.getAddress().getCountry(), dto.getAddress().getCountry());
    }

    @Test
    @DisplayName("toEntity should convert UserDto to UserEntity with complete user information")
    void toEntityShouldConvertUserDtoToUserEntityWithCompleteUserInformation() {
        UserDto dto = UserDtoTestStub.createUserDto();
        UserEntity entity = userDtoConverter.toEntity(dto);

        assertEquals(dto.getId(), entity.getId());
        assertEquals(dto.getFirstName(), entity.getFirstName());
        assertEquals(dto.getLastName(), entity.getLastName());
        assertEquals(dto.getEmail(), entity.getEmail());
        assertEquals(dto.getPassword(), entity.getPassword());
        assertEquals(dto.getStripeCustomerToken(), entity.getStripeCustomerToken());
        assertEquals(dto.getAddress().getLine(), entity.getAddress().getLine());
        assertEquals(dto.getAddress().getCity(), entity.getAddress().getCity());
        assertEquals(dto.getAddress().getCountry(), entity.getAddress().getCountry());
    }
}