package com.zufar.icedlatte.user.validator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class PutUsersRequestValidatorTest {

    private PutUsersRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PutUsersRequestValidator();
    }

    @Test
    void shouldNotThrowExceptionWhenAllParametersAreCorrect() {
        String firstName = "name";
        String lastName = "surname";
        String phoneNumber = "+79000000000";
        String birthDate = "2000-12-01";
        String addressJSONAsString = """
                {
                "country": "Country",
                "city": "City",
                "line": "Line",
                "postcode": "00000"
                }""";
        JsonObject addressJSON = JsonParser.parseString(addressJSONAsString).getAsJsonObject();

        assertDoesNotThrow(() -> validator.validate(firstName, lastName, phoneNumber, birthDate, addressJSON));
    }

    @Test
    void shouldNotThrowExceptionWhenHasOnlyRequiredParameters() {
        String firstName = "name";
        String lastName = "surname";

        assertDoesNotThrow(() -> validator.validate(firstName, lastName, null, null, null));
    }

    @Test
    void shouldThrowPutUserBadRequestExceptionWhenParametersAreIncorrect() {
        String firstName = null;
        String lastName = "s";
        String phoneNumber = "+7900000000b";
        String birthDate = "2000-12-011";
        String addressJSONAsString = """
                {
                "country": ["Country", "Another country"],
                "town": "City",
                "postcode": "00000"
                }""";
        JsonObject addressJSON = JsonParser.parseString(addressJSONAsString).getAsJsonObject();
        String expectedMessage = "PutUsersRequest parameters are incorrect. Error messages are [" +
                "  Error: { First name is required. }. " +
                " Error: { Last name must be between 2 and 64 characters. }. " +
                " Error: { Phone must be in international E.164 format, e.g. +12025550123. }. " +
                " Error: { Date of birth must be in format YYYY-MM-DD. }. " +
                " Error: { Unknown address field `town`. }. " +
                " Error: { Address field `country` must be a string. }.  " +
                "].";

        PutUsersBadRequestException thrownException = assertThrows(PutUsersBadRequestException.class, () -> validator.validate(firstName, lastName, phoneNumber, birthDate, addressJSON));


        assertEquals(expectedMessage, thrownException.getMessage());
    }
}
