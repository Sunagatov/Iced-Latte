package com.zufar.icedlatte.review.validator;

import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        StringBuilder errorMessages = new StringBuilder();

        StringBuilder paginationErrorMessages = paginationParametersValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection, ALLOWED_SORT_ATTRIBUTES_VALUES);
        errorMessages.append(paginationErrorMessages);

        StringBuilder productRatingsParameterErrorMessages = validateProductRatingsParameter(productRatings);
        errorMessages.append(productRatingsParameterErrorMessages);

        if (!errorMessages.isEmpty()) {
            throw new GetProductsBadRequestException(errorMessages.toString());
        }
    }

    private StringBuilder validateProductRatingsParameter(final List<Integer> productRatings) {
        final StringBuilder errorMessages = new StringBuilder();
        
        if (productRatings == null) {
            errorMessages.append(createNullRatingError());
            return errorMessages;
        }
        
        if (hasInvalidValues(productRatings)) {
            errorMessages.append(createInvalidValuesError(productRatings));
        }
        
        if (hasDuplicates(productRatings)) {
            errorMessages.append(createDuplicatesError(productRatings));
        }
        
        return errorMessages;
    }
    
    private String createNullRatingError() {
        return createErrorMessage(String.format("product's rating is required. Allowed 'productRating' values are '%s'.", ALLOWED_PRODUCT_RATING_VALUES));
    }
    
    private boolean hasInvalidValues(final List<Integer> productRatings) {
        return productRatings.stream().anyMatch(Objects::isNull) || !ALLOWED_PRODUCT_RATING_VALUES.containsAll(productRatings);
    }
    
    private String createInvalidValuesError(final List<Integer> productRatings) {
        return createErrorMessage(String.format("Some values of this product's rating list = '%s' are incorrect. Allowed 'productRating' values are '%s'.",
                productRatings, ALLOWED_PRODUCT_RATING_VALUES));
    }
    
    private boolean hasDuplicates(final List<Integer> productRatings) {
        return productRatings.stream().noneMatch(Objects::isNull) && productRatings.stream().distinct().count() < productRatings.size();
    }
    
    private String createDuplicatesError(final List<Integer> productRatings) {
        return createErrorMessage(String.format("This list of product's rating values '%s' has duplicates. Product's rating values must be unique.", productRatings));
    }

    private static String createErrorMessage(final String errorMessage) {
        return String.format(" Error: { %s }. ", errorMessage);
    }
}
