package com.zufar.icedlatte.review.validator;

import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetReviewsRequestValidator unit tests")
class GetReviewsRequestValidatorTest {

    private GetReviewsRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GetReviewsRequestValidator(new PaginationParametersValidator());
    }

    @Test
    @DisplayName("Valid request passes")
    void validate_allValid_noException() {
        assertThatCode(() -> validator.validate(0, 10, "createdAt", "desc", List.of(1, 2, 3)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Null productRatings is allowed")
    void validate_nullRatings_noException() {
        assertThatCode(() -> validator.validate(0, 10, "createdAt", "asc", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Invalid rating value throws")
    void validate_invalidRatingValue_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "createdAt", "asc", List.of(6)))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("productRating");
    }

    @Test
    @DisplayName("Duplicate rating values throws")
    void validate_duplicateRatings_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "createdAt", "asc", List.of(1, 1)))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("duplicates");
    }

    @Test
    @DisplayName("Null element in ratings list throws")
    void validate_nullElementInRatings_throws() {
        List<Integer> withNull = new java.util.ArrayList<>();
        withNull.add(1);
        withNull.add(null);
        assertThatThrownBy(() -> validator.validate(0, 10, "createdAt", "asc", withNull))
                .isInstanceOf(GetProductsBadRequestException.class);
    }

    @Test
    @DisplayName("Invalid sortAttribute throws")
    void validate_invalidSortAttribute_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "unknown", "asc", null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("sortAttribute");
    }

    @Test
    @DisplayName("All valid ratings 1-5 pass")
    void validate_allValidRatings_noException() {
        assertThatCode(() -> validator.validate(0, 10, "productRating", "desc", List.of(1, 2, 3, 4, 5)))
                .doesNotThrowAnyException();
    }
}
