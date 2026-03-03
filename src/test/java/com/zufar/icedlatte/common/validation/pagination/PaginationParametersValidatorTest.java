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
    @DisplayName("Returns empty builder when all params are valid")
    void validate_allValid_noErrors() {
        var result = validator.validate(0, 10, "name", "asc", allowed);
        assertThat(result.toString()).isEmpty();
    }

    @Test
    @DisplayName("Returns error when pageNumber is negative")
    void validate_negativePageNumber_hasError() {
        var result = validator.validate(-1, 10, "name", "asc", allowed);
        assertThat(result.toString()).contains("PageNumber");
    }

    @Test
    @DisplayName("Returns error when pageSize is zero")
    void validate_zeroPageSize_hasError() {
        var result = validator.validate(0, 0, "name", "asc", allowed);
        assertThat(result.toString()).contains("PageSize");
    }

    @Test
    @DisplayName("Returns error when pageSize is negative")
    void validate_negativePageSize_hasError() {
        var result = validator.validate(0, -5, "name", "asc", allowed);
        assertThat(result.toString()).contains("PageSize");
    }

    @Test
    @DisplayName("Returns error when sortAttribute is not in allowed set")
    void validate_invalidSortAttribute_hasError() {
        var result = validator.validate(0, 10, "unknown", "asc", allowed);
        assertThat(result.toString()).contains("sortAttribute");
    }

    @Test
    @DisplayName("Returns error when sortDirection is invalid")
    void validate_invalidSortDirection_hasError() {
        var result = validator.validate(0, 10, "name", "sideways", allowed);
        assertThat(result.toString()).contains("sortDirection");
    }

    @Test
    @DisplayName("sortDirection is case-insensitive")
    void validate_sortDirectionUpperCase_noError() {
        var result = validator.validate(0, 10, "name", "DESC", allowed);
        assertThat(result.toString()).isEmpty();
    }

    @Test
    @DisplayName("Null pageSize produces an error; other null params are skipped")
    void validate_nullPageSize_producesError() {
        var result = validator.validate(null, null, null, null, allowed);
        assertThat(result.toString()).contains("PageSize");
    }

    @Test
    @DisplayName("Non-null params with valid values produce no error")
    void validate_validParams_noErrors() {
        var result = validator.validate(null, 10, null, null, allowed);
        assertThat(result.toString()).isEmpty();
    }

    @Test
    @DisplayName("Accumulates multiple errors")
    void validate_multipleErrors_allReported() {
        var result = validator.validate(-1, 0, "bad", "sideways", allowed);
        String msg = result.toString();
        assertThat(msg).contains("PageNumber").contains("PageSize").contains("sortAttribute").contains("sortDirection");
    }
}
