package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteItem;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
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
public class DeleteProductsFromFavoriteListTest {

    @InjectMocks
    private DeleteProductsFromFavoriteList deleteProductsFromFavoriteList;

    @Mock
    private GetFavoriteList getFavoriteList;

    @Test
    @DisplayName("Should delete products from favorite list")
    void shouldDeleteProductsFromFavoriteList() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductInfo productInfo = new ProductInfo();
        productInfo.setProductId(productId);

        FavoriteItem favoriteItem = new FavoriteItem();
        favoriteItem.setProductInfo(productInfo);

        FavoriteList favoriteList = new FavoriteList();
        favoriteList.setFavoriteItems(new HashSet<>(Set.of(favoriteItem)));

        when(getFavoriteList.getEntityFavoriteList(userId)).thenReturn(favoriteList);

        assertDoesNotThrow(() -> deleteProductsFromFavoriteList.delete(productId, userId));

        verify(getFavoriteList, times(1)).getEntityFavoriteList(userId);
    }
}