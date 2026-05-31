package com.zufar.icedlatte.order.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrderSpecifications")
class OrderSpecificationsTest {

    @Test
    @DisplayName("does not restrict by user when userId is omitted")
    @SuppressWarnings("ConstantValue")
    void belongsToUserWithNullUserIdReturnsNoSpecification() {
        UUID userId = null;
        assertThat(OrderSpecifications.belongsToUser(userId)).isNull();
    }

    @Test
    @DisplayName("restricts by user when userId is provided")
    void belongsToUserWithUserIdReturnsSpecification() {
        assertThat(OrderSpecifications.belongsToUser(UUID.randomUUID())).isNotNull();
    }
}
