package com.zufar.icedlatte.user.validator;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PutUsersRequestValidator Tests")
class PutUsersRequestValidatorTest {

    private PutUsersRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PutUsersRequestValidator();
    }

    @Test
    @DisplayName("Should not throw exception when all parameters are correct")
    void shouldNotThrowExceptionWhenAllParametersAreCorrect() {
        AddressDto address = new AddressDto().country("Country").city("City").line("Line").postcode("00000");
        LocalDate birthDate = LocalDate.of(2000, 12, 1);

        assertDoesNotThrow(() -> validator.validate("name", "surname", "+79000000000", birthDate, address));
    }

    @Test
    @DisplayName("Should not throw exception when only required parameters are provided")
    void shouldNotThrowExceptionWhenHasOnlyRequiredParameters() {
        assertDoesNotThrow(() -> validator.validate("name", "surname", null, null, null));
    }

    @Test
    @DisplayName("Should throw BadRequestException when name and phone are incorrect")
    void shouldThrowWhenCoreParametersAreIncorrect() {
        assertThrows(BadRequestException.class, () ->
                validator.validate(null, "s", "+7900000000b", null, null));
    }
}
