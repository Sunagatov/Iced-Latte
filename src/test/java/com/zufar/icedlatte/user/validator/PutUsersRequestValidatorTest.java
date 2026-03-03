package com.zufar.icedlatte.user.validator;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        assertDoesNotThrow(() -> validator.validate("name", "surname", "+79000000000", "2000-12-01", address));
    }

    @Test
    @DisplayName("Should not throw exception when only required parameters are provided")
    void shouldNotThrowExceptionWhenHasOnlyRequiredParameters() {
        assertDoesNotThrow(() -> validator.validate("name", "surname", null, null, null));
    }

    @Test
    @DisplayName("Should throw PutUsersBadRequestException when name, phone and birthDate are incorrect")
    void shouldThrowWhenCoreParametersAreIncorrect() {
        assertThrows(PutUsersBadRequestException.class, () ->
                validator.validate(null, "s", "+7900000000b", "2000-12-011", null));
    }
}