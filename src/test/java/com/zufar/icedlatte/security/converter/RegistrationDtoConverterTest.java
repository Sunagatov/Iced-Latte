package com.zufar.icedlatte.security.converter;

import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RegistrationDtoConverterTest.Config.class})
class RegistrationDtoConverterTest {

    @Configuration
    public static class Config {

        @Bean
        public RegistrationDtoConverter registrationDtoConverter() {
            return Mappers.getMapper(RegistrationDtoConverter.class);
        }
    }

    @Autowired
    RegistrationDtoConverter registrationDtoConverter;

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

