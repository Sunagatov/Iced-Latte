package com.zufar.icedlatte.favorite.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FavoriteItemEntityTest {

    @Test
    @DisplayName("favorite items are equal by favorite list id and product id")
    void favoriteItemsAreEqualByFavoriteListIdAndProductId() {
        UUID favoriteListId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteItemEntity first = favoriteItem(favoriteListId, productId);
        FavoriteItemEntity second = favoriteItem(favoriteListId, productId);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    @DisplayName("favorite items with transient favorite lists are not equal")
    void transientFavoriteListItemsAreNotEqual() {
        UUID productId = UUID.randomUUID();

        FavoriteItemEntity first = favoriteItem(null, productId);
        FavoriteItemEntity second = favoriteItem(null, productId);

        assertThat(first).isNotEqualTo(second);
    }

    private static FavoriteItemEntity favoriteItem(UUID favoriteListId, UUID productId) {
        FavoriteListEntity favoriteList = new FavoriteListEntity();
        favoriteList.setId(favoriteListId);

        return FavoriteItemEntity.builder()
                .id(UUID.randomUUID())
                .favoriteListEntity(favoriteList)
                .productId(productId)
                .build();
    }
}
