package com.zufar.onlinestore;

import com.zufar.onlinestore.security.converter.RegistrationDtoConverter;
import com.zufar.onlinestore.security.dto.registration.UserRegistrationRequest;
import com.zufar.onlinestore.user.dto.UserDto;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RegistrationDtoConverterTest {

    @Autowired
    private RegistrationDtoConverter converter;
    private UserRegistrationRequest userRegistrationRequest;

    @BeforeEach
    void setUp() {
        userRegistrationRequest = Instancio.create(UserRegistrationRequest.class);
    }

    @Test
    void shouldCheckUserRegistrationRequestDoesNotEqualToNull() {
        assertNotNull(userRegistrationRequest);
    }

    @Test
    void shouldCheckUserDtoDoesNotEqualToNull() {
        UserDto dto = converter.toDto(userRegistrationRequest);

        assertNotNull(dto);
    }

    @Test
    void shouldMapUserRegistrationRequestToUserDto() {
        UserDto dto = converter.toDto(userRegistrationRequest);

        assertAll(
                () -> assertEquals(dto.firstName(), userRegistrationRequest.firstName()),
                () -> assertEquals(dto.lastName(), userRegistrationRequest.lastName()),
                () -> assertEquals(dto.username(), userRegistrationRequest.username()),
                () -> assertEquals(dto.email(), userRegistrationRequest.email()),
                () -> assertEquals(dto.password(), userRegistrationRequest.password())
        );
    }
}