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
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock
    private ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    @Mock
    private ProductPictureLinkUpdater productPictureLinkUpdater;

    private final ListOfFavoriteProducts listOfFavoriteProducts = new ListOfFavoriteProducts();

    @Test
    @DisplayName("Should add products to favorite list returning favorite list")
    void shouldAddProductsToFavoriteListReturningFavoriteList() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID favoriteListId = UUID.randomUUID();

        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(productId);

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
        ListOfFavoriteProductsDto expectedResponse = new ListOfFavoriteProductsDto();
        when(listOfFavoriteProductsDtoConverter.toListProductDto(expectedFavoriteListDto)).thenReturn(expectedResponse);

        ListOfFavoriteProductsDto result = favoriteProductAdder.add(listOfFavoriteProducts, userId);

        assertEquals(expectedResponse, result);

        verify(favoriteListProvider).getFavoriteListEntity(userId);
        verify(productInfoRepository).findAllById(any());
        verify(favoriteRepository).save(favoriteList);
        verify(favoriteListDtoConverter).toDto(favoriteList);
    }

    @Test
    @DisplayName("Should recover when concurrent favorite insert creates the same item")
    void shouldRecoverWhenConcurrentFavoriteInsertCreatesSameItem() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(productId);

        ProductInfoDto productInfoDto = new ProductInfoDto();
        productInfoDto.setId(productId);

        FavoriteListEntity staleFavoriteList = new FavoriteListEntity();
        staleFavoriteList.setFavoriteItems(new HashSet<>());

        FavoriteItemEntity existingFavoriteItem = new FavoriteItemEntity();
        existingFavoriteItem.setProductInfo(productInfo);

        FavoriteListEntity freshFavoriteList = new FavoriteListEntity();
        freshFavoriteList.setFavoriteItems(new HashSet<>(Set.of(existingFavoriteItem)));

        FavoriteListDto dto = new FavoriteListDto(
                UUID.randomUUID(),
                userId,
                Set.of(new FavoriteItemDto(UUID.randomUUID(), productInfoDto)),
                OffsetDateTime.now());
        ListOfFavoriteProductsDto response = new ListOfFavoriteProductsDto();
        response.setProducts(List.of(productInfoDto));

        listOfFavoriteProducts.setProductIds(List.of(productId));

        when(favoriteListProvider.getFavoriteListEntity(userId))
                .thenReturn(staleFavoriteList)
                .thenReturn(freshFavoriteList);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(productInfo));
        when(favoriteRepository.save(staleFavoriteList))
                .thenThrow(new DataIntegrityViolationException("uq_favorite_item_list_product"));
        when(favoriteRepository.save(freshFavoriteList)).thenReturn(freshFavoriteList);
        when(favoriteListDtoConverter.toDto(freshFavoriteList)).thenReturn(dto);
        when(listOfFavoriteProductsDtoConverter.toListProductDto(dto)).thenReturn(response);

        ListOfFavoriteProductsDto result = favoriteProductAdder.add(listOfFavoriteProducts, userId);

        assertEquals(response, result);
        verify(favoriteRepository).save(staleFavoriteList);
        verify(favoriteRepository).save(freshFavoriteList);
    }
}
