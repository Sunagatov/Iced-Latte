package com.zufar.icedlatte.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GetProductsRequestValidator {

    private static final Set<String> ALLOWED_SORT_ATTRIBUTES_VALUES =
            Set.of("name", "price", "quantity", "averageRating", "reviewsCount", "brandName", "sellerName");
    private static final Set<Integer> ALLOWED_MINIMUM_AVERAGE_RATING_VALUES = Set.of(1, 2, 3, 4, 5);
    private static final int MAX_KEYWORD_LENGTH = 200;

    public static void validate(
            final @Nullable Integer pageNumber,
            final @Nullable Integer pageSize,
            final @Nullable String sortAttribute,
            final @Nullable String sortDirection,
            final @Nullable BigDecimal minPrice,
            final @Nullable BigDecimal maxPrice,
            final @Nullable Integer minimumAverageRating,
            final @Nullable List<@Nullable String> brandNames,
            final @Nullable List<@Nullable String> sellerNames,
            final @Nullable String keyword) {

        List<String> errors = new ArrayList<>(PaginationParametersValidator.validate(
                pageNumber, pageSize, sortAttribute, sortDirection, ALLOWED_SORT_ATTRIBUTES_VALUES));
        errors.addAll(validateMinMaxPrice(minPrice, maxPrice));
        errors.addAll(validateNameList(brandNames, "brandNames"));
        errors.addAll(validateNameList(sellerNames, "sellerNames"));
        errors.addAll(validateKeyword(keyword));
        if (minimumAverageRating != null && !ALLOWED_MINIMUM_AVERAGE_RATING_VALUES.contains(minimumAverageRating)) {
            errors.add(error("'%s' is incorrect 'minimumAverageRating' value. Allowed values are '%s'."
                    .formatted(minimumAverageRating, ALLOWED_MINIMUM_AVERAGE_RATING_VALUES)));
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.format(
                    "GetProductsRequest parameters are incorrect. Error messages are [ %s ].",
                    String.join(" ", errors)));
        }
    }

    private static List<String> validateMinMaxPrice(@Nullable BigDecimal minPrice, @Nullable BigDecimal maxPrice) {
        List<String> errors = new ArrayList<>();
        if (minPrice != null && minPrice.signum() < 0) {
            errors.add(error("'%s' is incorrect 'minPrice'. It must be a non-negative number.".formatted(minPrice)));
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            errors.add(error("'%s' is incorrect 'maxPrice'. It must be a non-negative number.".formatted(maxPrice)));
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            errors.add(error(
                    "'%s' and '%s' are incorrect. 'maxPrice' must be >= 'minPrice'.".formatted(minPrice, maxPrice)));
        }
        return errors;
    }

    private static List<String> validateNameList(@Nullable List<@Nullable String> names, String fieldName) {
        List<String> errors = new ArrayList<>();
        if (names != null && names.stream().anyMatch(Objects::isNull)) {
            errors.add(error("Some values of '%s' are null. Values must be non-null.".formatted(fieldName)));
        }
        if (names != null && names.stream().filter(Objects::nonNull).anyMatch(String::isBlank)) {
            errors.add(error("Some values of '%s' are blank. Values must be non-blank.".formatted(fieldName)));
        }
        if (names != null && names.stream().distinct().count() < names.size()) {
            errors.add(error("'%s' has duplicates. Values must be unique.".formatted(fieldName)));
        }
        return errors;
    }

    private static List<String> validateKeyword(@Nullable String keyword) {
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            return List.of(error("'%s' is too long 'keyword'. Maximum length is %s characters."
                    .formatted(keyword, MAX_KEYWORD_LENGTH)));
        }
        return List.of();
    }

    private static String error(String message) {
        return " Error: { %s }. ".formatted(message);
    }
}
