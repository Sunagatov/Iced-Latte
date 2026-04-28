package com.zufar.icedlatte.common.validation.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaginationParametersValidator unit tests")
class PaginationParametersValidatorTest {

    private final PaginationParametersValidator validator = new PaginationParametersValidator();
    private final Set<String> allowed = Set.of("name", "price", "rating");

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("returns no errors when all parameters are valid")
        void returnsNoErrorsWhenAllParametersAreValid() {
            assertThat(validator.validate(0, 10, "name", "asc", allowed)).isEmpty();
        }

        @Test
        @DisplayName("allows null optional parameters")
        void allowsNullOptionalParameters() {
            assertThat(validator.validate(null, null, null, null, allowed)).isEmpty();
        }

        @Test
        @DisplayName("reports negative page number")
        void reportsNegativePageNumber() {
            List<String> errors = validator.validate(-1, 10, "name", "asc", allowed);

            assertThat(errors).singleElement().asString().contains("PageNumber");
        }

        @Test
        @DisplayName("reports zero page size")
        void reportsZeroPageSize() {
            List<String> errors = validator.validate(0, 0, "name", "asc", allowed);

            assertThat(errors).singleElement().asString().contains("PageSize");
        }

        @Test
        @DisplayName("reports invalid sort attribute")
        void reportsInvalidSortAttribute() {
            List<String> errors = validator.validate(0, 10, "unknown", "asc", allowed);

            assertThat(errors).singleElement().asString().contains("sortAttribute");
        }

        @Test
        @DisplayName("treats sort direction case-insensitively")
        void treatsSortDirectionCaseInsensitively() {
            assertThat(validator.validate(0, 10, "name", "DESC", allowed)).isEmpty();
        }

        @Test
        @DisplayName("reports invalid sort direction")
        void reportsInvalidSortDirection() {
            List<String> errors = validator.validate(0, 10, "name", "sideways", allowed);

            assertThat(errors).singleElement().asString().contains("sortDirection");
        }

        @Test
        @DisplayName("accumulates all validation errors")
        void accumulatesAllValidationErrors() {
            List<String> errors = validator.validate(-1, 0, "bad", "sideways", allowed);

            assertThat(errors).hasSize(4);
            assertThat(String.join(" ", errors))
                    .contains("PageNumber")
                    .contains("PageSize")
                    .contains("sortAttribute")
                    .contains("sortDirection");
        }
    }
}
