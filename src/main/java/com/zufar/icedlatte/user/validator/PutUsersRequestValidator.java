package com.zufar.icedlatte.user.validator;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
                         LocalDate birthDate,
                         AddressDto addressDto) {
        List<String> errors = new ArrayList<>();

        validateName(firstName, "First name", errors);
        validateName(lastName, "Last name", errors);
        validatePhone(phoneNumber, errors);
        validateBirthDate(birthDate, errors);
        validateAddress(addressDto, errors);

        if (!errors.isEmpty()) {
            throw new PutUsersBadRequestException(String.join(" ", errors));
        }
    }

    private void validateName(String name, String label, List<String> errors) {
        if (name == null) {
            errors.add(error(label + " is required."));
        } else if (name.isBlank()) {
            errors.add(error(label + " must not be blank."));
        } else if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            errors.add(error(String.format("%s must be between %d and %d characters.", label, MIN_NAME_LENGTH, MAX_NAME_LENGTH)));
        } else if (!name.matches("^[a-zA-Z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u00FF\\s'\\u2019\\-]+$")) {
            errors.add(error(label + " can only contain letters, spaces, hyphens, and apostrophes."));
        }
    }

    private void validatePhone(String phoneNumber, List<String> errors) {
        if (phoneNumber != null && !phoneNumber.isBlank() && !phoneNumber.matches(PHONE_REGEXP)) {
            errors.add(error(PHONE_ERROR));
        }
    }

    private void validateBirthDate(LocalDate birthDate, List<String> errors) {
        if (birthDate == null) return;
        if (!birthDate.isBefore(LocalDate.now())) {
            errors.add(error("Date of birth must be in the past."));
        } else if (birthDate.isAfter(LocalDate.now().minusYears(13))) {
            errors.add(error("You must be at least 13 years old."));
        }
    }

    private void validateAddress(AddressDto addressDto, List<String> errors) {
        if (addressDto == null) return;
        boolean anyFieldPresent = addressDto.getCountry() != null
                || addressDto.getCity() != null
                || addressDto.getLine() != null
                || addressDto.getPostcode() != null;
        if (anyFieldPresent) {
            validateAddressField(addressDto.getCountry(), "country", errors);
            validateAddressField(addressDto.getCity(), "city", errors);
            validateAddressField(addressDto.getLine(), "line", errors);
            validateAddressField(addressDto.getPostcode(), "postcode", errors);
        }
    }

    private void validateAddressField(String value, String fieldName, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(error(String.format("Address field `%s` is required and must not be blank.", fieldName)));
        }
    }

    private static String error(String message) {
        return String.format(" Error: { %s }. ", message);
    }
}
