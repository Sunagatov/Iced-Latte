package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteProductDeleterTest {

    @InjectMocks
    private FavoriteProductDeleter favoriteProductDeleter;

    @Mock
    private FavoriteListProvider favoriteListProvider;

    @Test
    @DisplayName("Should delete products from favorite list")
    void shouldDeleteProductsFromFavoriteList() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductInfo productInfo = new ProductInfo();
        productInfo.setProductId(productId);

        FavoriteItemEntity favoriteItem = new FavoriteItemEntity();
        favoriteItem.setProductInfo(productInfo);

        FavoriteListEntity favoriteList = new FavoriteListEntity();
        favoriteList.setFavoriteItems(new HashSet<>(Set.of(favoriteItem)));

        when(favoriteListProvider.getFavoriteListEntity(userId)).thenReturn(favoriteList);

        assertDoesNotThrow(() -> favoriteProductDeleter.delete(productId, userId));

        verify(favoriteListProvider, times(1)).getFavoriteListEntity(userId);
    }
}