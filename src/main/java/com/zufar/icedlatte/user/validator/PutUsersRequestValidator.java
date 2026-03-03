package com.zufar.icedlatte.user.validator;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class PutUsersRequestValidator {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 64;
    private static final String PHONE_REGEXP = "^\\+[1-9]\\d{6,14}$";
    private static final String PHONE_ERROR = "Phone must be in international E.164 format, e.g. +12025550123.";

    public void validate(String firstName,
                         String lastName,
                         String phoneNumber,
                         String birthDate,
                         AddressDto addressDto) {
        StringBuilder errorMessages = new StringBuilder();

        errorMessages.append(validateNameParameter(firstName, "First name"));
        errorMessages.append(validateNameParameter(lastName, "Last name"));
        errorMessages.append(validatePhoneParameter(phoneNumber));
        errorMessages.append(validateBirthDateParameter(birthDate));
        errorMessages.append(validateAddressParameter(addressDto));

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
        } else if (!name.matches("^[a-zA-Z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u00FF\\s'\\u2019\\-]+$")) {
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

    private StringBuilder validateAddressParameter(AddressDto addressDto) {
        StringBuilder errorMessages = new StringBuilder();
        if (addressDto != null) {
            validateAddressField(errorMessages, addressDto.getCountry(), "country");
            validateAddressField(errorMessages, addressDto.getCity(), "city");
            validateAddressField(errorMessages, addressDto.getLine(), "line");
            validateAddressField(errorMessages, addressDto.getPostcode(), "postcode");
        }
        return errorMessages;
    }

    private void validateAddressField(StringBuilder errors, String value, String fieldName) {
        if (value != null && value.isBlank()) {
            errors.append(createErrorMessage(String.format("Address field `%s` must not be blank.", fieldName)));
        }
    }

    private String createErrorMessage(String errorMessage) {
        return String.format(" Error: { %s }. ", errorMessage);
    }
}