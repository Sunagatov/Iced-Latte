package com.zufar.icedlatte.common.validation.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PaginationParametersValidator {

    private static final Set<String> ALLOWED_SORT_DIRECTION_VALUES = Set.of("asc", "desc");

    public static List<String> validate(
            final @Nullable Integer pageNumber,
            final @Nullable Integer pageSize,
            final @Nullable String sortAttribute,
            final @Nullable String sortDirection,
            final Set<String> allowedSortAttributeValues) {
        List<String> errors = new ArrayList<>();
        if (pageNumber != null && pageNumber < 0) {
            String errorMessage = "'%s' is the incorrect 'PageNumber' attribute value. 'PageNumber' value should be non negative integer number value.";
            errors.add(error(errorMessage.formatted(pageNumber)));
        }
        if (pageSize != null && pageSize < 1) {
            String errorMessage = "'%s' is the incorrect 'PageSize' attribute value. 'PageSize' value should be a positive integer.";
            errors.add(error(errorMessage.formatted(pageSize)));
        }
        String normalizedSortAttribute = sortAttribute == null ? null : sortAttribute.trim();
        if (normalizedSortAttribute != null && !allowedSortAttributeValues.contains(normalizedSortAttribute)) {
            String errorMessage = "'%s' is incorrect 'sortAttribute' value. Allowed 'sortAttribute' values are '%s'.";
            errors.add(error(errorMessage.formatted(sortAttribute, allowedSortAttributeValues)));
        }
        String normalizedSortDirection = sortDirection == null ? null : sortDirection.trim();
        if (normalizedSortDirection != null
                && !ALLOWED_SORT_DIRECTION_VALUES.contains(normalizedSortDirection.toLowerCase(java.util.Locale.ROOT))) {
            String errorMessage = "'%s' is incorrect 'sortDirection' value. Allowed 'sortDirection' values are '%s'.";
            errors.add(error(errorMessage.formatted(sortDirection, ALLOWED_SORT_DIRECTION_VALUES)));
        }
        return errors;
    }

    private static String error(String message) {
        return " Error: { %s }. ".formatted(message);
    }
}
