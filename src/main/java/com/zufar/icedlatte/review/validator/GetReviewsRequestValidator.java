package com.zufar.icedlatte.review.validator;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetReviewsRequestValidator {

    private static final Set<String> ALLOWED_SORT_ATTRIBUTES_VALUES = Set.of("createdAt", "productRating");
    private static final Set<Integer> ALLOWED_PRODUCT_RATING_VALUES = Set.of(1, 2, 3, 4, 5);

    private final PaginationParametersValidator paginationParametersValidator;

    public void validate(final Integer pageNumber,
                         final Integer pageSize,
                         final String sortAttribute,
                         final String sortDirection,
                         final List<Integer> productRatings) {
        List<String> errors = new ArrayList<>(
                paginationParametersValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection, ALLOWED_SORT_ATTRIBUTES_VALUES));

        if (productRatings != null) {
            if (productRatings.stream().anyMatch(Objects::isNull) || !ALLOWED_PRODUCT_RATING_VALUES.containsAll(productRatings)) {
                errors.add(error("Some values of this product's rating list = '%s' are incorrect. Allowed 'productRating' values are '%s'."
                        .formatted(productRatings, ALLOWED_PRODUCT_RATING_VALUES)));
            } else if (productRatings.stream().distinct().count() < productRatings.size()) {
                errors.add(error("This list of product's rating values '%s' has duplicates. Product's rating values must be unique."
                        .formatted(productRatings)));
            }
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.format(
                    "GetReviewsRequest parameters are incorrect. Error messages are [ %s ].",
                    String.join(" ", errors)));
        }
    }

    private static String error(String message) {
        return " Error: { %s }. ".formatted(message);
    }
}
