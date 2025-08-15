package com.zufar.icedlatte.product.validator;

import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        StringBuilder errorMessages = new StringBuilder();
        errorMessages.append(paginationParametersValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection, ALLOWED_SORT_ATTRIBUTES_VALUES));
        errorMessages.append(validateMinMaxPriceParameter(minPrice, maxPrice));
        errorMessages.append(validateBrandNameList(brandNames));
        errorMessages.append(validateSellerNameList(sellerNames));
        errorMessages.append(validateMinimumAverageRatingParameter(minimumAverageRating));

        if (!errorMessages.isEmpty()) {
            throw new GetProductsBadRequestException(errorMessages.toString());
        }
    }

    private StringBuilder validateMinimumAverageRatingParameter(final Integer minimumAverageRating) {
        StringBuilder errorMessages = new StringBuilder();
        if (minimumAverageRating != null && !ALLOWED_MINIMUM_AVERAGE_RATING_VALUES.contains(minimumAverageRating)) {
            String msg = "'%s' is incorrect 'minimumAverageRating' value. Allowed values are '%s'."
                    .formatted(minimumAverageRating, ALLOWED_MINIMUM_AVERAGE_RATING_VALUES);
            errorMessages.append(createErrorMessage(msg));
        }
        return errorMessages;
    }

    private static StringBuilder validateMinMaxPriceParameter(BigDecimal minPrice, BigDecimal maxPrice) {
        final StringBuilder errors = new StringBuilder();
        if (minPrice != null && minPrice.signum() < 0) {
            errors.append(createErrorMessage("'%s' is incorrect 'minPrice'. It must be a non-negative number."
                    .formatted(minPrice)));
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            errors.append(createErrorMessage("'%s' is incorrect 'maxPrice'. It must be a non-negative number."
                    .formatted(maxPrice)));
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            errors.append(createErrorMessage("'%s' and '%s' are incorrect. 'maxPrice' must be >= 'minPrice'."
                    .formatted(minPrice, maxPrice)));
        }
        return errors;
    }

    private static StringBuilder validateBrandNameList(List<String> brandNames) {
        final StringBuilder errors = new StringBuilder();
        if (brandNames != null && brandNames.stream().anyMatch(StringUtils::isBlank)) {
            errors.append(createErrorMessage("Some values of 'brandNames' are blank. Values must be non-blank."));
        }
        if (brandNames != null && brandNames.stream().distinct().count() < brandNames.size()) {
            errors.append(createErrorMessage("'brandNames' has duplicates. Values must be unique."));
        }
        return errors;
    }

    private static StringBuilder validateSellerNameList(List<String> sellerNames) {
        final StringBuilder errors = new StringBuilder();
        if (sellerNames != null && sellerNames.stream().anyMatch(StringUtils::isBlank)) {
            errors.append(createErrorMessage("Some values of 'sellerNames' are blank. Values must be non-blank."));
        }
        if (sellerNames != null && sellerNames.stream().distinct().count() < sellerNames.size()) {
            errors.append(createErrorMessage("'sellerNames' has duplicates. Values must be unique."));
        }
        return errors;
    }

    private static String createErrorMessage(String errorMessage) {
        return " Error: { %s }. ".formatted(errorMessage);
    }
}
