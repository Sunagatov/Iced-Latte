package com.zufar.icedlatte.common.validation.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaginationParametersValidator unit tests")
class PaginationParametersValidatorTest {

    private final PaginationParametersValidator validator = new PaginationParametersValidator();
    private final Set<String> allowed = Set.of("name", "price", "rating");

    @Test
    @DisplayName("Returns empty list when all params are valid")
    void validate_allValid_noErrors() {
        assertThat(validator.validate(0, 10, "name", "asc", allowed)).isEmpty();
    }

    @Test
    @DisplayName("Returns error when pageNumber is negative")
    void validate_negativePageNumber_hasError() {
        assertThat(validator.validate(-1, 10, "name", "asc", allowed))
                .anyMatch(e -> e.contains("PageNumber"));
    }

    @Test
    @DisplayName("Returns error when pageSize is zero")
    void validate_zeroPageSize_hasError() {
        assertThat(validator.validate(0, 0, "name", "asc", allowed))
                .anyMatch(e -> e.contains("PageSize"));
    }

    @Test
    @DisplayName("Returns error when pageSize is negative")
    void validate_negativePageSize_hasError() {
        assertThat(validator.validate(0, -5, "name", "asc", allowed))
                .anyMatch(e -> e.contains("PageSize"));
    }

    @Test
    @DisplayName("Returns error when sortAttribute is not in allowed set")
    void validate_invalidSortAttribute_hasError() {
        assertThat(validator.validate(0, 10, "unknown", "asc", allowed))
                .anyMatch(e -> e.contains("sortAttribute"));
    }

    @Test
    @DisplayName("Returns error when sortDirection is invalid")
    void validate_invalidSortDirection_hasError() {
        assertThat(validator.validate(0, 10, "name", "sideways", allowed))
                .anyMatch(e -> e.contains("sortDirection"));
    }

    @Test
    @DisplayName("sortDirection is case-insensitive")
    void validate_sortDirectionUpperCase_noError() {
        assertThat(validator.validate(0, 10, "name", "DESC", allowed)).isEmpty();
    }

    @Test
    @DisplayName("Null pageSize is valid (caller omitted it; provider applies default)")
    void validate_nullPageSize_noError() {
        assertThat(validator.validate(null, null, null, null, allowed))
                .noneMatch(e -> e.contains("PageSize"));
    }

    @Test
    @DisplayName("Non-null params with valid values produce no error")
    void validate_validParams_noErrors() {
        assertThat(validator.validate(null, 10, null, null, allowed)).isEmpty();
    }

    @Test
    @DisplayName("Accumulates multiple errors")
    void validate_multipleErrors_allReported() {
        var errors = validator.validate(-1, 0, "bad", "sideways", allowed);
        String joined = String.join(" ", errors);
        assertThat(joined)
                .contains("PageNumber")
                .contains("PageSize")
                .contains("sortAttribute")
                .contains("sortDirection");
    }
}
