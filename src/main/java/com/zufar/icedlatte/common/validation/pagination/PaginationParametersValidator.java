package com.zufar.icedlatte.common.validation.pagination;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PaginationParametersValidator {

    private static final Set<String> ALLOWED_SORT_DIRECTION_VALUES = Set.of("asc", "desc");

    public List<String> validate(final Integer pageNumber,
                                 final Integer pageSize,
                                 final String sortAttribute,
                                 final String sortDirection,
                                 final Set<String> allowedSortAttributeValues) {
        List<String> errors = new ArrayList<>();
        if (pageNumber != null && pageNumber < 0) {
            errors.add(error("'%s' is the incorrect 'PageNumber' attribute value. 'PageNumber' value should be non negative integer number value.".formatted(pageNumber)));
        }
        if (pageSize != null && pageSize < 1) {
            errors.add(error("'%s' is the incorrect 'PageSize' attribute value. 'PageSize' value should be non negative integer number value which is bigger than 1.".formatted(pageSize)));
        }
        if (sortAttribute != null && !allowedSortAttributeValues.contains(sortAttribute)) {
            errors.add(error("'%s' is incorrect 'sortAttribute' value. Allowed 'sortAttribute' values are '%s'.".formatted(sortAttribute, allowedSortAttributeValues)));
        }
        if (sortDirection != null && !ALLOWED_SORT_DIRECTION_VALUES.contains(sortDirection.toLowerCase(java.util.Locale.ROOT))) {
            errors.add(error("'%s' is incorrect 'sortDirection' value. Allowed 'sortDirection' values are '%s'.".formatted(sortDirection, ALLOWED_SORT_DIRECTION_VALUES)));
        }
        return errors;
    }

    private static String error(String message) {
        return " Error: { %s }. ".formatted(message);
    }
}
