package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FavoriteProductAdderTest {

    @InjectMocks
    private FavoriteProductAdder favoriteProductAdder;

    @Mock
    private FavoriteListProvider favoriteListProvider;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private FavoriteListDtoConverter favoriteListDtoConverter;

    private ListOfFavoriteProducts listOfFavoriteProducts = new ListOfFavoriteProducts();

    @Test
    @DisplayName("Should add products to favorite list returning favorite list")
    void shouldAddProductsToFavoriteListReturningFavoriteList() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID favoriteListId = UUID.randomUUID();

        ProductInfo productInfo = new ProductInfo();
        productInfo.setProductId(productId);

        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(productId);

        FavoriteItemEntity favoriteItem = new FavoriteItemEntity();
        favoriteItem.setProductInfo(productInfo);

        FavoriteItemDto favoriteItemDto = new FavoriteItemDto(
                UUID.randomUUID(),
                productInfoDto);

        FavoriteListEntity favoriteList = new FavoriteListEntity();
        favoriteList.setFavoriteItems(new HashSet<>());

        FavoriteListEntity addedFavoriteList = new FavoriteListEntity();
        addedFavoriteList.setFavoriteItems(Set.of(favoriteItem));

        FavoriteListDto expectedFavoriteListDto = new FavoriteListDto(
                favoriteListId,
                userId,
                Set.of(favoriteItemDto),
                OffsetDateTime.now());

        listOfFavoriteProducts.setProductIds(List.of(productId));

        when(favoriteListProvider.getFavoriteListEntity(userId)).thenReturn(favoriteList);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(productInfo));
        when(favoriteRepository.save(favoriteList)).thenReturn(addedFavoriteList);
        when(favoriteListDtoConverter.toDto(favoriteList)).thenReturn(expectedFavoriteListDto);

        FavoriteListDto result = favoriteProductAdder.add(listOfFavoriteProducts, userId);

        assertEquals(expectedFavoriteListDto, result);

        verify(favoriteListProvider, times(1)).getFavoriteListEntity(userId);
        verify(productInfoRepository, times(1)).findAllById(any());
        verify(favoriteRepository, times(1)).save(favoriteList);
        verify(favoriteListDtoConverter, times(1)).toDto(favoriteList);
    }
}