package com.zufar.icedlatte.common.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageRequestFactory unit tests")
class PageRequestFactoryTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("creates pageable with ascending sort and id tiebreaker")
        void createsPageableWithAscendingSortAndIdTiebreaker() {
            Pageable pageable = PageRequestFactory.of(0, 10, "name", "asc");

            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort()).containsExactly(
                    Sort.Order.asc("name"),
                    Sort.Order.asc("id")
            );
        }

        @Test
        @DisplayName("creates pageable with descending primary sort and ascending id tiebreaker")
        void createsPageableWithDescendingPrimarySortAndAscendingIdTiebreaker() {
            Pageable pageable = PageRequestFactory.of(2, 5, "price", "desc");

            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort()).containsExactly(
                    Sort.Order.desc("price"),
                    Sort.Order.asc("id")
            );
        }

        @Test
        @DisplayName("treats uppercase DESC as descending")
        void treatsUppercaseDescAsDescending() {
            Pageable pageable = PageRequestFactory.of(0, 20, "createdAt", "DESC");

            assertThat(pageable.getSort()).containsExactly(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id")
            );
        }

        @Test
        @DisplayName("does not append duplicate id tiebreaker when sorting by id")
        void doesNotAppendDuplicateIdTiebreakerWhenSortingById() {
            Pageable pageable = PageRequestFactory.of(3, 15, "id", "asc");

            assertThat(pageable.getPageNumber()).isEqualTo(3);
            assertThat(pageable.getPageSize()).isEqualTo(15);
            assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("id"));
        }
    }
}
