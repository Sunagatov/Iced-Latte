package com.zufar.icedlatte.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Utils unit tests")
class UtilsTest {

    @Test
    @DisplayName("Creates pageable with ascending sort")
    void createPageableObject_ascDirection_returnsAscendingSort() {
        Pageable pageable = Utils.createPageableObject(0, 10, "name", "asc");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        Sort.Order nameOrder = pageable.getSort().getOrderFor("name");
        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Creates pageable with descending sort")
    void createPageableObject_descDirection_returnsDescendingSort() {
        Pageable pageable = Utils.createPageableObject(2, 5, "price", "desc");

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        Sort.Order priceOrder = pageable.getSort().getOrderFor("price");
        assertThat(priceOrder).isNotNull();
        assertThat(priceOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Non-ASC direction defaults to descending")
    void createPageableObject_unknownDirection_treatedAsDesc() {
        Pageable pageable = Utils.createPageableObject(0, 20, "createdAt", "DESC");

        Sort.Order createdAtOrder = pageable.getSort().getOrderFor("createdAt");
        assertThat(createdAtOrder).isNotNull();
        assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Page number and size are correctly set")
    void createPageableObject_pageAndSize_setCorrectly() {
        Pageable pageable = Utils.createPageableObject(3, 15, "id", "asc");

        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(15);
    }
}
