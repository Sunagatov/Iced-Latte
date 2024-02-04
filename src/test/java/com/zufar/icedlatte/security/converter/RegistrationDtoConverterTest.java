package com.zufar.icedlatte.security.converter;

import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.converter.AddressDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RegistrationDtoConverterTest {

    @InjectMocks
    private RegistrationDtoConverter registrationDtoConverter;

    @Mock
    private AddressDtoConverter addressDtoConverter;

    @Test
    @DisplayName("ToDto should convert UserRegistrationRequest to UserDto with accurate data")
    void shouldConvertUserRegistrationRequestToUserDtoWithAccurateData() {
        UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest(
                "John", "Doe", "john.doe@example.com", "password123"
        );

        UserEntity userEntity = registrationDtoConverter.toEntity(userRegistrationRequest);

        assertEquals(userRegistrationRequest.firstName(), userEntity.getFirstName());
        assertEquals(userRegistrationRequest.lastName(), userEntity.getLastName());
        assertEquals(userRegistrationRequest.email(), userEntity.getEmail());
        assertEquals(userRegistrationRequest.password(), userEntity.getPassword());
    }
}

