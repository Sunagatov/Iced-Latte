package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.converter.ListOfFavoriteProductsDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteProductAdderTest {

    @InjectMocks
    private FavoriteProductAdder favoriteProductAdder;

    @Mock private FavoriteListProvider favoriteListProvider;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private ProductCatalogApi productCatalogApi;
    @Mock private FavoriteListDtoConverter favoriteListDtoConverter;
    @Mock private ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    @Mock private ProductPictureLinkUpdater productPictureLinkUpdater;

    @Test
    @DisplayName("Should add products to favorite list returning favorite list")
    void shouldAddProductsToFavoriteListReturningFavoriteList() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(productId);

        FavoriteListEntity favoriteList = new FavoriteListEntity();
        favoriteList.setFavoriteItems(new HashSet<>());

        FavoriteItemEntity savedItem = FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(productId).build();
        FavoriteListEntity savedList = new FavoriteListEntity();
        savedList.setFavoriteItems(Set.of(savedItem));

        FavoriteListDto dto = new FavoriteListDto(UUID.randomUUID(), userId,
                Set.of(new FavoriteItemDto(UUID.randomUUID(), productInfoDto)), OffsetDateTime.now());
        ListOfFavoriteProductsDto expectedResponse = new ListOfFavoriteProductsDto();

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(productId));

        when(favoriteListProvider.getFavoriteListEntity(userId)).thenReturn(favoriteList);
        when(productCatalogApi.getProductsByIds(any())).thenReturn(List.of(productInfoDto));
        when(favoriteRepository.save(favoriteList)).thenReturn(savedList);
        when(favoriteListDtoConverter.toDto(any(FavoriteListEntity.class), anyMap())).thenReturn(dto);
        when(listOfFavoriteProductsDtoConverter.toListProductDto(dto)).thenReturn(expectedResponse);

        ListOfFavoriteProductsDto result = favoriteProductAdder.add(request, userId);

        assertEquals(expectedResponse, result);
        verify(favoriteListProvider).getFavoriteListEntity(userId);
        verify(favoriteRepository).save(favoriteList);
    }

    @Test
    @DisplayName("Should recover when concurrent favorite insert creates the same item")
    void shouldRecoverWhenConcurrentFavoriteInsertCreatesSameItem() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(productId);

        FavoriteListEntity staleFavoriteList = new FavoriteListEntity();
        staleFavoriteList.setId(UUID.randomUUID());
        staleFavoriteList.setFavoriteItems(new HashSet<>());

        FavoriteItemEntity existingItem = FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(productId).build();
        FavoriteListEntity freshFavoriteList = new FavoriteListEntity();
        freshFavoriteList.setId(UUID.randomUUID());
        freshFavoriteList.setFavoriteItems(new HashSet<>(Set.of(existingItem)));

        FavoriteListDto dto = new FavoriteListDto(UUID.randomUUID(), userId,
                Set.of(new FavoriteItemDto(UUID.randomUUID(), productInfoDto)), OffsetDateTime.now());
        ListOfFavoriteProductsDto response = new ListOfFavoriteProductsDto();
        response.setProducts(List.of(productInfoDto));

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(productId));

        when(favoriteListProvider.getFavoriteListEntity(userId))
                .thenReturn(staleFavoriteList)
                .thenReturn(freshFavoriteList);
        when(productCatalogApi.getProductsByIds(any())).thenReturn(List.of(productInfoDto));
        when(favoriteRepository.save(staleFavoriteList))
                .thenThrow(new DataIntegrityViolationException("uq_favorite_item_list_product"));
        when(favoriteRepository.save(freshFavoriteList)).thenReturn(freshFavoriteList);
        when(favoriteListDtoConverter.toDto(any(FavoriteListEntity.class), anyMap())).thenReturn(dto);
        when(listOfFavoriteProductsDtoConverter.toListProductDto(dto)).thenReturn(response);

        ListOfFavoriteProductsDto result = favoriteProductAdder.add(request, userId);

        assertEquals(response, result);
        verify(favoriteRepository).save(staleFavoriteList);
        verify(favoriteRepository).save(freshFavoriteList);
    }
}
