package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteProductDeleterTest {

    @InjectMocks
    private FavoriteProductDeleter favoriteProductDeleter;

    @Mock
    private FavoriteListProvider favoriteListProvider;

    @Test
    @DisplayName("Should remove product from favorite list when list exists")
    void shouldDeleteProductsFromFavoriteList() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteItemEntity favoriteItem = FavoriteItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .build();

        FavoriteListEntity favoriteList = new FavoriteListEntity();
        favoriteList.setFavoriteItems(new HashSet<>(Set.of(favoriteItem)));

        when(favoriteListProvider.findFavoriteListEntity(userId)).thenReturn(Optional.of(favoriteList));

        assertDoesNotThrow(() -> favoriteProductDeleter.delete(productId, userId));

        verify(favoriteListProvider).findFavoriteListEntity(userId);
        assertTrue(favoriteList.getFavoriteItems().isEmpty());
    }

    @Test
    @DisplayName("Should be a no-op when no favorite list exists for user")
    void shouldDoNothingWhenFavoriteListAbsent() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(favoriteListProvider.findFavoriteListEntity(userId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> favoriteProductDeleter.delete(productId, userId));

        verify(favoriteListProvider).findFavoriteListEntity(userId);
    }
}
