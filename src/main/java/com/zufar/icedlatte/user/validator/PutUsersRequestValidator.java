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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class PutUsersRequestValidator {

    private final int MIN_LENGTH = 2;
    private final int MAX_LENGTH = 128;

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
            String errorMessage = String.format("%s is the mandatory attribute.", parameterTypeForErrorMessage);
            errorMessages.append(createErrorMessage(errorMessage));
        } else if (name.length() < MIN_LENGTH || name.length() > MAX_LENGTH) {
            String errorMessage = String.format("%s should have a length between %d and %d characters.", parameterTypeForErrorMessage, MIN_LENGTH, MAX_LENGTH);
            errorMessages.append(createErrorMessage(errorMessage));
        }
        return errorMessages;
    }

    private StringBuilder validatePhoneParameter(String phoneNumber) {
        StringBuilder errorMessages = new StringBuilder();
        if (phoneNumber != null) {
            String phoneTemplateRegexp = "^\\+?[1-9]\\d{1,14}$";
            if (!phoneNumber.matches(phoneTemplateRegexp)) {
                String errorMessage = "Phone should contain only digits. The first symbol is allowed to be \"+\".";
                errorMessages.append(createErrorMessage(errorMessage));
            }
        }
        return errorMessages;
    }

    private StringBuilder validateBirthDateParameter(String birthDate) {
        StringBuilder errorMessages = new StringBuilder();
        if (birthDate != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
                LocalDate localDate = LocalDate.parse(birthDate, formatter);
            } catch (DateTimeParseException exception) {
                errorMessages.append(createErrorMessage("Birth date should be in format YYYY-MM-DD."));
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
            Collections.sort(allFieldNames);

            Set<Entry<String, JsonElement>> entries = addressJsonObject.entrySet();
            for (Entry<String, JsonElement> entry : entries) {
                if (!allFieldNames.contains(entry.getKey())) {
                    String errorMessage = String.format("The field `%s` in the JSON string is not defined in the `AddressDto` properties. JSON: %s", entry.getKey(), addressJsonObject.toString());
                    errorMessages.append(createErrorMessage(errorMessage));
                }
            }
            // all fields are required and primitive
            for (String name : allFieldNames) {
                JsonElement jsonElement = addressJsonObject.get(name);
                if (jsonElement == null || jsonElement.isJsonNull()) {
                    String errorMessage = String.format("The required field `%s` is not found in the JSON string: %s", name, addressJsonObject.toString());
                    errorMessages.append(createErrorMessage(errorMessage));
                } else if (!jsonElement.isJsonPrimitive()) {
                    String errorMessage = String.format("Expected the field `%s` to be a primitive type in the JSON string but got `%s`", name, jsonElement.toString());
                    errorMessages.append(createErrorMessage(errorMessage));
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
