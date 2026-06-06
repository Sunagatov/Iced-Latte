package com.zufar.icedlatte.user.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.AddressDto;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PutUsersRequestValidator {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_ADDRESS_FIELD_LENGTH = 128;
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u00FF\\s'\\u2019\\-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final String PHONE_ERROR = "Phone must be in international E.164 format, e.g. +12025550123.";

    public static void validate(
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable String phoneNumber,
            @Nullable LocalDate birthDate,
            @Nullable AddressDto addressDto) {
        validate(firstName, lastName, phoneNumber, birthDate, addressDto, LocalDate.now());
    }

    static void validate(
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable String phoneNumber,
            @Nullable LocalDate birthDate,
            @Nullable AddressDto addressDto,
            LocalDate today) {
        List<String> errors = new ArrayList<>();

        validateName(firstName, "First name", errors);
        validateName(lastName, "Last name", errors);
        validatePhone(phoneNumber, errors);
        validateBirthDate(birthDate, today, errors);
        validateAddress(addressDto, errors);

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.format(
                    "PutUsersRequest parameters are incorrect. Error messages are [ %s ].", String.join(" ", errors)));
        }
    }

    private static void validateName(@Nullable String name, String label, List<String> errors) {
        if (name == null) {
            errors.add(error(label + " is required."));
            return;
        }

        if (name.isBlank()) {
            errors.add(error(label + " must not be blank."));
            return;
        }

        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            errors.add(error(
                    "%s must be between %d and %d characters.".formatted(label, MIN_NAME_LENGTH, MAX_NAME_LENGTH)));
            return;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            errors.add(error(label + " can only contain letters, spaces, hyphens, and apostrophes."));
        }
    }

    private static void validatePhone(@Nullable String phoneNumber, List<String> errors) {
        if (phoneNumber != null
                && !phoneNumber.isBlank()
                && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            errors.add(error(PHONE_ERROR));
        }
    }

    private static void validateBirthDate(@Nullable LocalDate birthDate, LocalDate today, List<String> errors) {
        if (birthDate == null) {
            return;
        }
        if (!birthDate.isBefore(today)) {
            errors.add(error("Date of birth must be in the past."));
        } else if (birthDate.isAfter(today.minusYears(13))) {
            errors.add(error("You must be at least 13 years old."));
        }
    }

    private static void validateAddress(@Nullable AddressDto addressDto, List<String> errors) {
        if (addressDto == null || isEmptyAddress(addressDto)) {
            return;
        }
        validateAddressField(addressDto.getCountry(), "country", errors);
        validateAddressField(addressDto.getCity(), "city", errors);
        validateAddressField(addressDto.getLine(), "line", errors);
        validateAddressField(addressDto.getPostcode(), "postcode", errors);
    }

    private static boolean isEmptyAddress(AddressDto addressDto) {
        return !StringUtils.hasText(addressDto.getCountry())
                && !StringUtils.hasText(addressDto.getCity())
                && !StringUtils.hasText(addressDto.getLine())
                && !StringUtils.hasText(addressDto.getPostcode());
    }

    private static void validateAddressField(@Nullable String value, String fieldName, List<String> errors) {
        if (value == null || value.isBlank()) {
            String errorMessage = "Address field `%s` is required and must not be blank.";
            errors.add(error(String.format(errorMessage, fieldName)));
            return;
        }
        if (value.length() > MAX_ADDRESS_FIELD_LENGTH) {
            String errorMessage = "Address field `%s` must not exceed %d characters.";
            errors.add(error(String.format(errorMessage, fieldName, MAX_ADDRESS_FIELD_LENGTH)));
        }
    }

    private static String error(String message) {
        return " Error: { %s }. ".formatted(message);
    }
}
