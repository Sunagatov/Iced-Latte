package com.zufar.icedlatte.order.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;

@DisplayName("OrderPageRequestFactory")
class OrderPageRequestFactoryTest {

    private final OrderPageRequestFactory factory = new OrderPageRequestFactory(
            new PaginationConfig(
                    0,
                    new PaginationConfig.Products(50, "name", "desc"),
                    new PaginationConfig.Reviews(10, "createdAt", "desc"),
                    new PaginationConfig.Orders(10, 50, "createdAt", "desc")),
            new PaginationParametersValidator());

    @Test
    @DisplayName("builds pageable with defaults")
    void buildsPageableWithDefaults() {
        var pageable = factory.build(null, null, null, null);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("rejects invalid query parameters as bad request")
    void rejectsInvalidQueryParametersAsBadRequest() {
        assertThatThrownBy(() -> factory.build(-1, 0, "unknown", "sideways"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Order pagination parameters are incorrect")
                .hasMessageContaining("PageNumber")
                .hasMessageContaining("PageSize")
                .hasMessageContaining("sortAttribute")
                .hasMessageContaining("sortDirection");
    }

    @Test
    @DisplayName("rejects page size above configured maximum")
    void rejectsPageSizeAboveConfiguredMaximum() {
        assertThatThrownBy(() -> factory.build(0, 51, "createdAt", "desc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum allowed 'size' value is '50'");
    }
}
