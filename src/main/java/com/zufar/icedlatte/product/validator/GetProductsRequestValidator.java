package com.zufar.icedlatte.product.validator;

import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetProductsRequestValidator {

    private static final Set<String> ALLOWED_SORT_ATTRIBUTES_VALUES =
            Set.of("name", "price", "quantity", "averageRating", "reviewsCount", "brandName", "sellerName");
    private static final Set<Integer> ALLOWED_MINIMUM_AVERAGE_RATING_VALUES = Set.of(1, 2, 3, 4);

    private final PaginationParametersValidator paginationParametersValidator;

    public void validate(final Integer pageNumber,
                         final Integer pageSize,
                         final String sortAttribute,
                         final String sortDirection,
                         final BigDecimal minPrice,
                         final BigDecimal maxPrice,
                         final Integer minimumAverageRating,
                         final List<String> brandNames,
                         final List<String> sellerNames) {

        List<String> errors = new ArrayList<>(paginationParametersValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection, ALLOWED_SORT_ATTRIBUTES_VALUES));
        errors.addAll(validateMinMaxPrice(minPrice, maxPrice));
        errors.addAll(validateNameList(brandNames, "brandNames"));
        errors.addAll(validateNameList(sellerNames, "sellerNames"));
        if (minimumAverageRating != null && !ALLOWED_MINIMUM_AVERAGE_RATING_VALUES.contains(minimumAverageRating)) {
            errors.add(error("'%s' is incorrect 'minimumAverageRating' value. Allowed values are '%s'."
                    .formatted(minimumAverageRating, ALLOWED_MINIMUM_AVERAGE_RATING_VALUES)));
        }

        if (!errors.isEmpty()) {
            throw new GetProductsBadRequestException(String.join(" ", errors));
        }
    }

    private static List<String> validateMinMaxPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        List<String> errors = new ArrayList<>();
        if (minPrice != null && minPrice.signum() < 0) {
            errors.add(error("'%s' is incorrect 'minPrice'. It must be a non-negative number.".formatted(minPrice)));
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            errors.add(error("'%s' is incorrect 'maxPrice'. It must be a non-negative number.".formatted(maxPrice)));
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            errors.add(error("'%s' and '%s' are incorrect. 'maxPrice' must be >= 'minPrice'.".formatted(minPrice, maxPrice)));
        }
        return errors;
    }

    private static List<String> validateNameList(List<String> names, String fieldName) {
        List<String> errors = new ArrayList<>();
        if (names != null && names.stream().anyMatch(StringUtils::isBlank)) {
            errors.add(error("Some values of '%s' are blank. Values must be non-blank.".formatted(fieldName)));
        }
        if (names != null && names.stream().distinct().count() < names.size()) {
            errors.add(error("'%s' has duplicates. Values must be unique.".formatted(fieldName)));
        }
        return errors;
    }

    private static String error(String message) {
        return " Error: { %s }. ".formatted(message);
    }
}
