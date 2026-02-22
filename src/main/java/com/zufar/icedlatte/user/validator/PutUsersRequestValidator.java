package com.zufar.icedlatte.user.validator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PutUsersRequestValidator {

    private final int MIN_NAME_LENGTH = 2;
    private final int MAX_NAME_LENGTH = 64;
    private final String PHONE_REGEXP = "^\\+[1-9]\\d{6,14}$";
    private final String PHONE_ERROR = "Phone must be in international E.164 format, e.g. +12025550123.";

    public void validate(String firstName,
                         String lastName,
                         String phoneNumber,
                         String birthDate,
                         JsonObject addressDto) {
        StringBuilder errorMessages = new StringBuilder();

        StringBuilder firstNameParameterMessages = validateNameParameter(firstName, "First name");
        errorMessages.append(firstNameParameterMessages);

        StringBuilder secondNameParameterMessages = validateNameParameter(lastName, "Last name");
        errorMessages.append(secondNameParameterMessages);

        StringBuilder phoneNumberParameterMessages = validatePhoneParameter(phoneNumber);
        errorMessages.append(phoneNumberParameterMessages);

        StringBuilder birthDateParameterMessages = validateBirthDateParameter(birthDate);
        errorMessages.append(birthDateParameterMessages);

        StringBuilder addressJSONParameterMessages = validateAddressJSONParameter(addressDto);
        errorMessages.append(addressJSONParameterMessages);

        if (!errorMessages.isEmpty()) {
            throw new PutUsersBadRequestException(errorMessages.toString());
        }
    }

    private StringBuilder validateNameParameter(String name, String parameterTypeForErrorMessage) {
        StringBuilder errorMessages = new StringBuilder();
        if (name == null) {
            errorMessages.append(createErrorMessage(parameterTypeForErrorMessage + " is required."));
        } else if (name.isBlank()) {
            errorMessages.append(createErrorMessage(parameterTypeForErrorMessage + " must not be blank."));
        } else if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            errorMessages.append(createErrorMessage(String.format("%s must be between %d and %d characters.", parameterTypeForErrorMessage, MIN_NAME_LENGTH, MAX_NAME_LENGTH)));
        } else if (!name.matches("^[a-zA-Z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u00FF\\s''\\-]+$")) {
            errorMessages.append(createErrorMessage(parameterTypeForErrorMessage + " can only contain letters, spaces, hyphens, and apostrophes."));
        }
        return errorMessages;
    }

    private StringBuilder validatePhoneParameter(String phoneNumber) {
        StringBuilder errorMessages = new StringBuilder();
        if (phoneNumber != null && !phoneNumber.isBlank() && !phoneNumber.matches(PHONE_REGEXP)) {
            errorMessages.append(createErrorMessage(PHONE_ERROR));
        }
        return errorMessages;
    }

    private StringBuilder validateBirthDateParameter(String birthDate) {
        StringBuilder errorMessages = new StringBuilder();
        if (birthDate != null && !birthDate.isBlank()) {
            try {
                LocalDate localDate = LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE);
                if (!localDate.isBefore(LocalDate.now())) {
                    errorMessages.append(createErrorMessage("Date of birth must be in the past."));
                } else if (localDate.isAfter(LocalDate.now().minusYears(13))) {
                    errorMessages.append(createErrorMessage("You must be at least 13 years old."));
                }
            } catch (DateTimeParseException e) {
                errorMessages.append(createErrorMessage("Date of birth must be in format YYYY-MM-DD."));
            }
        }
        return errorMessages;
    }

    private StringBuilder validateAddressJSONParameter(JsonObject addressJsonObject) {
        StringBuilder errorMessages = new StringBuilder();
        if (addressJsonObject != null) {
            List<Field> allFields = List.of(AddressDto.class.getDeclaredFields());
            List<String> allFieldNames = allFields.stream()
                    .map(field -> getFieldNameFromDeclaredField(field.getName()))
                    .collect(Collectors.toList());

            for (Entry<String, JsonElement> entry : addressJsonObject.entrySet()) {
                if (!allFieldNames.contains(entry.getKey())) {
                    errorMessages.append(createErrorMessage(String.format(
                            "Unknown address field `%s`.", entry.getKey())));
                }
            }

            for (String name : allFieldNames) {
                JsonElement jsonElement = addressJsonObject.get(name);
                if (jsonElement != null && !jsonElement.isJsonNull() && !jsonElement.isJsonPrimitive()) {
                    errorMessages.append(createErrorMessage(String.format(
                            "Address field `%s` must be a string.", name)));
                }
            }
        }
        return errorMessages;
    }

    private String createErrorMessage(String errorMessage) {
        return String.format(" Error: { %s }. ", errorMessage);
    }

    private String getFieldNameFromDeclaredField(String declaredFieldName) {
        return declaredFieldName.substring(declaredFieldName.lastIndexOf(".") + 1);
    }
}
