package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.excaption.FavoritesPageException;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FavoriteListPageProviderTest {

    @InjectMocks
    private FavoriteListPageProvider favoriteListPageProvider;

    @Mock
    private ProductInfoDtoConverter productInfoDtoConverter;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Test
    @DisplayName("should return list of favorite list by page")
    public void shouldReturnListOfFavoriteListByPage() {
        UUID userId = UUID.randomUUID();
        Integer page = 2;

        ProductInfo testProductInfo = new ProductInfo();
        ProductInfoDto testProductInfoDto = new ProductInfoDto();

        FavoriteItemEntity testFavoriteItemEntity = new FavoriteItemEntity();
        testFavoriteItemEntity.setProductInfo(testProductInfo);

        List<FavoriteItemEntity> favoriteItemEntities = Collections.singletonList(testFavoriteItemEntity);
        List<ProductInfoDto> expectedResult = Collections.singletonList(testProductInfoDto);

        when(favoriteRepository.findFavoriteItemsByUserIdWithPagination(eq(userId), any(Pageable.class))).thenReturn(favoriteItemEntities);
        when(productInfoDtoConverter.toDto(testProductInfo)).thenReturn(testProductInfoDto);

        List<ProductInfoDto> result = favoriteListPageProvider.getFavoritesProductsByPage(userId, page);

        assertEquals(expectedResult, result);

        verify(favoriteRepository, times(1)).findFavoriteItemsByUserIdWithPagination(eq(userId), any(Pageable.class));
        verify(productInfoDtoConverter, times(1)).toDto(testProductInfo);
    }

    @Test
    @DisplayName("should throw exception when list of favorite list by page")
    public void shouldThrowExceptionWhenListOfFavoriteListByPage() {
        UUID userId = UUID.randomUUID();
        Integer page = 2;

        List<FavoriteItemEntity> favoriteItemEntities = new ArrayList<>();

        when(favoriteRepository.findFavoriteItemsByUserIdWithPagination(eq(userId), any(Pageable.class))).thenReturn(favoriteItemEntities);

        assertThrows(FavoritesPageException.class, () -> favoriteListPageProvider.getFavoritesProductsByPage(userId, page));
    }
}