package com.zufar.icedlatte.security.converter;

import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationDtoConverterTest {

    RegistrationDtoConverter registrationDtoConverter;

    @BeforeEach
    void setUp() {
        registrationDtoConverter = Mappers.getMapper(RegistrationDtoConverter.class);
    }

    @Test
    @DisplayName("ToDto should convert UserRegistrationRequest to UserDto with accurate data")
    void shouldConvertUserRegistrationRequestToUserDtoWithAccurateData() {
        UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest(
                "John", "Doe", "john.doe@example.com", "password123"
        );

        UserEntity userEntity = registrationDtoConverter.toEntity(userRegistrationRequest);

        assertEquals(userRegistrationRequest.getFirstName(), userEntity.getFirstName());
        assertEquals(userRegistrationRequest.getLastName(), userEntity.getLastName());
        assertEquals(userRegistrationRequest.getEmail(), userEntity.getEmail());
        assertEquals(userRegistrationRequest.getPassword(), userEntity.getPassword());
    }
}

