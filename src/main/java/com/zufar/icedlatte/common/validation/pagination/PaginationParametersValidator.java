package com.zufar.icedlatte.common.validation.pagination;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PaginationParametersValidator {

    private static final Set<String> ALLOWED_SORT_DIRECTION_VALUES = Set.of("asc", "desc");

    public StringBuilder validate(final Integer pageNumber,
                                  final Integer pageSize,
                                  final String sortAttribute,
                                  final String sortDirection,
                                  final Set<String> allowedSortAttributeValues) {
        final StringBuilder errorMessages = new StringBuilder();
        if (pageNumber != null && pageNumber < 0) {
            String errorMessage = String.format("'%s' is the incorrect 'PageNumber' attribute value. " +
                    "'PageNumber' value should be non negative integer number value.", pageNumber);
            errorMessages.append(createErrorMessage(errorMessage));
        }
        if (pageSize == null) {
            String errorMessage = "PageSize value should not be null or empty. Please provide some numeric value.";
            errorMessages.append(createErrorMessage(errorMessage));
        }
        if (pageSize != null && pageSize < 1) {
            String errorMessage = String.format("'%s' is the incorrect 'PageSize' attribute value. " +
                    "'PageSize' value should be non negative integer number value which is bigger than 1.", pageSize);
            errorMessages.append(createErrorMessage(errorMessage));
        }
        if (sortAttribute != null && !allowedSortAttributeValues.contains(sortAttribute)) {
            String errorMessage = String.format("'%s' is incorrect 'sortAttribute' value. Allowed 'sortAttribute' values are '%s'.",
                    sortAttribute, allowedSortAttributeValues);
            errorMessages.append(createErrorMessage(errorMessage));
        }
        if (sortDirection != null && !ALLOWED_SORT_DIRECTION_VALUES.contains(sortDirection.toLowerCase(java.util.Locale.ROOT))) {
            String errorMessage = String.format("'%s' is incorrect 'sortDirection' value. Allowed 'sortDirection' values are '%s'.",
                    sortDirection, ALLOWED_SORT_DIRECTION_VALUES);
            errorMessages.append(createErrorMessage(errorMessage));
        }
        return errorMessages;
    }

    private static String createErrorMessage(String errorMessage) {
        return String.format(" Error: { %s }. ", errorMessage);
    }
}
