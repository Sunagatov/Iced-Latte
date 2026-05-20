package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.converter.ListOfFavoriteProductsDtoConverter;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService unit tests")
class FavoriteServiceTest {

    @InjectMocks private FavoriteService favoriteService;

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private ProductCatalogApi productCatalogApi;
    @Mock private FavoriteListDtoConverter favoriteListDtoConverter;
    @Mock private ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    @Mock private ProductPictureLinkUpdater productPictureLinkUpdater;

    @Test
    @DisplayName("getEnrichedFavoriteList returns enriched DTO when list exists")
    void getEnrichedFavoriteListReturnsDto() {
        UUID userId = UUID.randomUUID();
        FavoriteListEntity entity = new FavoriteListEntity();
        entity.setFavoriteItems(new HashSet<>());
        ListOfFavoriteProductsDto response = new ListOfFavoriteProductsDto();
        response.setProducts(List.of());

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(productCatalogApi.getProductsByIds(any())).thenReturn(List.of());
        when(favoriteListDtoConverter.toDto(any(), anyMap()))
                .thenReturn(new FavoriteListDto(UUID.randomUUID(), userId, Set.of(), OffsetDateTime.now()));
        when(listOfFavoriteProductsDtoConverter.toListProductDto(any())).thenReturn(response);

        var result = favoriteService.getEnrichedFavoriteList(userId);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("add validates products and persists new favorites")
    void addPersistsNewFavorites() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteListEntity entity = new FavoriteListEntity();
        entity.setFavoriteItems(new HashSet<>());

        ProductInfoDto productDto = new ProductInfoDto();
        productDto.setId(productId);

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(productId));

        ListOfFavoriteProductsDto response = new ListOfFavoriteProductsDto();
        response.setProducts(List.of(productDto));

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(productCatalogApi.getProductsByIds(any())).thenReturn(List.of(productDto));
        when(favoriteRepository.save(entity)).thenReturn(entity);
        when(favoriteListDtoConverter.toDto(any(), anyMap()))
                .thenReturn(new FavoriteListDto(UUID.randomUUID(), userId, Set.of(), OffsetDateTime.now()));
        when(listOfFavoriteProductsDtoConverter.toListProductDto(any())).thenReturn(response);

        var result = favoriteService.add(request, userId);

        assertThat(result).isSameAs(response);
        verify(favoriteRepository).save(entity);
    }

    @Test
    @DisplayName("add recovers from concurrent insert conflict")
    void addRecoversFromConcurrentConflict() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteListEntity staleList = new FavoriteListEntity();
        staleList.setFavoriteItems(new HashSet<>());

        FavoriteItemEntity existingItem = FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(productId).build();
        FavoriteListEntity freshList = new FavoriteListEntity();
        freshList.setFavoriteItems(new HashSet<>(Set.of(existingItem)));

        ProductInfoDto productDto = new ProductInfoDto();
        productDto.setId(productId);

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(productId));

        ListOfFavoriteProductsDto response = new ListOfFavoriteProductsDto();
        response.setProducts(List.of(productDto));

        when(favoriteRepository.findByUserId(userId))
                .thenReturn(Optional.of(staleList))
                .thenReturn(Optional.of(freshList));
        when(productCatalogApi.getProductsByIds(any())).thenReturn(List.of(productDto));
        when(favoriteRepository.save(staleList))
                .thenThrow(new DataIntegrityViolationException("uq_favorite_item_list_product"));
        when(favoriteRepository.save(freshList)).thenReturn(freshList);
        when(favoriteListDtoConverter.toDto(any(), anyMap()))
                .thenReturn(new FavoriteListDto(UUID.randomUUID(), userId, Set.of(), OffsetDateTime.now()));
        when(listOfFavoriteProductsDtoConverter.toListProductDto(any())).thenReturn(response);

        var result = favoriteService.add(request, userId);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("delete removes product from favorite list")
    void deleteRemovesProduct() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteItemEntity item = FavoriteItemEntity.builder().id(UUID.randomUUID()).productId(productId).build();
        FavoriteListEntity entity = new FavoriteListEntity();
        entity.setFavoriteItems(new HashSet<>(Set.of(item)));

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(entity));

        assertDoesNotThrow(() -> favoriteService.delete(productId, userId));
        assertTrue(entity.getFavoriteItems().isEmpty());
    }

    @Test
    @DisplayName("delete is a no-op when no favorite list exists")
    void deleteNoOpWhenListAbsent() {
        UUID userId = UUID.randomUUID();
        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> favoriteService.delete(UUID.randomUUID(), userId));
        verify(favoriteRepository, never()).save(any());
    }
}
