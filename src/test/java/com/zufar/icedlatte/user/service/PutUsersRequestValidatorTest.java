package com.zufar.icedlatte.user.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.AddressDto;

@DisplayName("PutUsersRequestValidator Tests")
class PutUsersRequestValidatorTest {

    @Test
    @DisplayName("Should not throw exception when all parameters are correct")
    void shouldNotThrowExceptionWhenAllParametersAreCorrect() {
        AddressDto address =
                new AddressDto().country("Country").city("City").line("Line").postcode("00000");
        LocalDate birthDate = LocalDate.of(2000, 12, 1);

        assertDoesNotThrow(
                () -> PutUsersRequestValidator.validate("name", "surname", "+79000000000", birthDate, address));
    }

    @Test
    @DisplayName("Should not throw exception when only required parameters are provided")
    void shouldNotThrowExceptionWhenHasOnlyRequiredParameters() {
        assertDoesNotThrow(() -> PutUsersRequestValidator.validate("name", "surname", null, null, null));
    }

    @Test
    @DisplayName("Should throw BadRequestException when name and phone are incorrect")
    void shouldThrowWhenCoreParametersAreIncorrect() {
        assertThrows(
                BadRequestException.class,
                () -> PutUsersRequestValidator.validate(null, "s", "+7900000000b", null, null));
    }

    @Test
    @DisplayName("Should throw BadRequestException when address field exceeds contract length")
    void shouldThrowWhenAddressFieldExceedsContractLength() {
        AddressDto address = new AddressDto()
                .country("Country")
                .city("City")
                .line("a".repeat(129))
                .postcode("00000");

        assertThrows(
                BadRequestException.class,
                () -> PutUsersRequestValidator.validate(
                        "name", "surname", "+79000000000", LocalDate.of(2000, 12, 1), address));
    }
}
