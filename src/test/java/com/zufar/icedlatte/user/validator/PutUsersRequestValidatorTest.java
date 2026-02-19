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
        String phoneNumber = "+7900000000";
        String birthDate = "2000-12-01";
        String addressJSONAsString = "{\n" +
                "\"country\": \"Country\",\n" +
                "\"city\": \"City\",\n" +
                "\"line\": \"Line\",\n" +
                "\"postcode\": \"00000\"\n" +
                "}";
        JsonObject addressJSON =  new JsonParser().parse(addressJSONAsString).getAsJsonObject();

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
        String addressJSONAsString = "{\n" +
                "\"country\": [\"Country\", \"Another country\"],\n" +
                "\"town\": \"City\",\n" +
                "\"postcode\": \"00000\"\n" +
                "}";
        JsonObject addressJSON =  new JsonParser().parse(addressJSONAsString).getAsJsonObject();
        String expectedMessage = String.format("PutUsersRequest parameters are incorrect. Error messages are [ " +
                " Error: { First name is the mandatory attribute. }. " +
                " Error: { Last name should have a length between 2 and 128 characters. }. " +
                " Error: { Phone should contain only digits. The first symbol is allowed to be \"+\". }. " +
                " Error: { Birth date should be in format YYYY-MM-DD. }. " +
                " Error: { The field `town` in the JSON string is not defined in the `AddressDto` properties. JSON: %s }. " +
                " Error: { Expected the field `country` to be a primitive type in the JSON string but got `[\"Country\",\"Another country\"]` }. " +
                " Error: { The required field `city` is not found in the JSON string: %s }. " +
                " Error: { The required field `line` is not found in the JSON string: %s }. " +
                " ].", addressJSON.toString(), addressJSON, addressJSON);

        PutUsersBadRequestException thrownException = assertThrows(PutUsersBadRequestException.class, () -> validator.validate(firstName, lastName, phoneNumber, birthDate, addressJSON));


        assertEquals(expectedMessage, thrownException.getMessage());
    }
}
