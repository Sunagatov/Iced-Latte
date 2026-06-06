package com.zufar.icedlatte.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.exception.FavoriteProductNotFoundException;
import com.zufar.icedlatte.favorite.exception.InvalidFavoriteRequestException;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService unit tests")
class FavoriteServiceTest {

    @InjectMocks
    private FavoriteService favoriteService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductCatalogApi productCatalogApi;

    @Test
    @DisplayName("getEnrichedFavoriteList returns enriched DTO when list exists")
    void getEnrichedFavoriteListReturnsDto() {
        UUID userId = UUID.randomUUID();
        FavoriteListEntity entity = new FavoriteListEntity();
        entity.setFavoriteItems(new HashSet<>());
        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(entity));

        var result = favoriteService.getEnrichedFavoriteList(userId);

        assertThat(result.getProducts()).isEmpty();
    }

    @Test
    @DisplayName("getEnrichedFavoriteList returns empty DTO when list does not exist")
    void getEnrichedFavoriteListReturnsEmptyDtoWhenListDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var result = favoriteService.getEnrichedFavoriteList(userId);

        assertThat(result.getProducts()).isEmpty();
        verifyNoInteractions(productCatalogApi);
    }

    @Test
    @DisplayName("add validates products and persists new favorites")
    void addPersistsNewFavorites() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteListEntity entity = favoriteList(userId);

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(productId));

        FavoriteListEntity refreshed = favoriteList(userId);
        refreshed
                .getFavoriteItems()
                .add(FavoriteItemEntity.builder()
                        .id(UUID.randomUUID())
                        .favoriteListEntity(refreshed)
                        .productId(productId)
                        .build());

        doReturn(Optional.of(entity))
                .doReturn(Optional.of(refreshed))
                .when(favoriteRepository)
                .findByUserId(userId);
        when(productCatalogApi.findExistingProductIds(Set.of(productId))).thenReturn(Set.of(productId));
        when(productCatalogApi.getProductsByIds(List.of(productId))).thenReturn(List.of(productSnapshot(productId)));

        var result = favoriteService.add(request, userId);

        assertThat(result.getProducts())
                .extracting(com.zufar.icedlatte.openapi.dto.ProductSummaryDto::getId)
                .containsExactly(productId);
        verify(favoriteRepository).insertFavoriteItemIfAbsent(any(UUID.class), eq(entity.getId()), eq(productId));
        verify(favoriteRepository).touchFavoriteList(entity.getId());
        verify(favoriteRepository).flush();
    }

    @Test
    @DisplayName("add validates distinct requested products in request order")
    void addValidatesDistinctRequestedProductsInRequestOrder() {
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondProductId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        FavoriteListEntity entity = favoriteList(userId);

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(firstProductId, firstProductId, secondProductId));

        FavoriteListEntity refreshed = favoriteList(userId);
        refreshed.getFavoriteItems().add(favoriteItem(refreshed, firstProductId));
        refreshed.getFavoriteItems().add(favoriteItem(refreshed, secondProductId));

        doReturn(Optional.of(entity))
                .doReturn(Optional.of(refreshed))
                .when(favoriteRepository)
                .findByUserId(userId);
        when(productCatalogApi.findExistingProductIds(new HashSet<>(List.of(firstProductId, secondProductId))))
                .thenReturn(Set.of(firstProductId, secondProductId));
        when(productCatalogApi.getProductsByIds(List.of(firstProductId, secondProductId)))
                .thenReturn(List.of(productSnapshot(firstProductId), productSnapshot(secondProductId)));

        var result = favoriteService.add(request, userId);

        assertThat(result.getProducts())
                .extracting(com.zufar.icedlatte.openapi.dto.ProductSummaryDto::getId)
                .containsExactly(firstProductId, secondProductId);
        verify(favoriteRepository).insertFavoriteItemIfAbsent(any(UUID.class), eq(entity.getId()), eq(firstProductId));
        verify(favoriteRepository).insertFavoriteItemIfAbsent(any(UUID.class), eq(entity.getId()), eq(secondProductId));
    }

    @Test
    @DisplayName("add creates favorite list before adding first item")
    void addCreatesFavoriteListBeforeAddingFirstItem() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteListEntity createdList = favoriteList(userId);

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(productId));

        FavoriteListEntity refreshed = favoriteList(userId);
        refreshed.getFavoriteItems().add(favoriteItem(refreshed, productId));

        when(favoriteRepository.findByUserId(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdList))
                .thenReturn(Optional.of(refreshed));
        when(productCatalogApi.findExistingProductIds(Set.of(productId))).thenReturn(Set.of(productId));
        when(productCatalogApi.getProductsByIds(List.of(productId))).thenReturn(List.of(productSnapshot(productId)));

        var result = favoriteService.add(request, userId);

        assertThat(result.getProducts())
                .extracting(com.zufar.icedlatte.openapi.dto.ProductSummaryDto::getId)
                .containsExactly(productId);
        verify(favoriteRepository).insertFavoriteListIfAbsent(any(UUID.class), eq(userId));
        verify(favoriteRepository).insertFavoriteItemIfAbsent(any(UUID.class), eq(createdList.getId()), eq(productId));
    }

    @Test
    @DisplayName("add throws favorite-owned exception for missing products")
    void addThrowsFavoriteExceptionForMissingProducts() {
        UUID userId = UUID.randomUUID();
        UUID missingProductId = UUID.randomUUID();
        FavoriteListEntity entity = favoriteList(userId);

        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of(missingProductId));

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(productCatalogApi.findExistingProductIds(Set.of(missingProductId))).thenReturn(Set.of());

        assertThatThrownBy(() -> favoriteService.add(request, userId))
                .isInstanceOf(FavoriteProductNotFoundException.class)
                .hasMessageContaining(missingProductId.toString());
        verify(favoriteRepository, never()).insertFavoriteItemIfAbsent(any(), any(), any());
    }

    @Test
    @DisplayName("add rejects oversized favorite requests")
    void addRejectsOversizedFavoriteRequests() {
        UUID userId = UUID.randomUUID();
        List<UUID> productIds = Stream.generate(UUID::randomUUID).limit(101).toList();
        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(productIds);

        assertThatThrownBy(() -> favoriteService.add(request, userId))
                .isInstanceOf(InvalidFavoriteRequestException.class)
                .hasMessageContaining("100");
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    @DisplayName("add rejects null favorite request product ids")
    void addRejectsNullFavoriteRequestProductIds() {
        UUID userId = UUID.randomUUID();
        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(null);

        assertThatThrownBy(() -> favoriteService.add(request, userId))
                .isInstanceOf(InvalidFavoriteRequestException.class)
                .hasMessageContaining("must not be null");
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    @DisplayName("add rejects empty favorite request product ids")
    void addRejectsEmptyFavoriteRequestProductIds() {
        UUID userId = UUID.randomUUID();
        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(List.of());

        assertThatThrownBy(() -> favoriteService.add(request, userId))
                .isInstanceOf(InvalidFavoriteRequestException.class)
                .hasMessageContaining("at least one");
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    @DisplayName("add rejects null values in favorite request product ids")
    void addRejectsNullValuesInFavoriteRequestProductIds() {
        UUID userId = UUID.randomUUID();
        ListOfFavoriteProducts request = new ListOfFavoriteProducts();
        request.setProductIds(new ArrayList<>(Collections.singletonList(null)));

        assertThatThrownBy(() -> favoriteService.add(request, userId))
                .isInstanceOf(InvalidFavoriteRequestException.class)
                .hasMessageContaining("must not contain null values");
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    @DisplayName("delete removes product from favorite list")
    void deleteRemovesProduct() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        FavoriteItemEntity item = FavoriteItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .build();
        FavoriteListEntity entity = new FavoriteListEntity();
        entity.setId(UUID.randomUUID());
        entity.setFavoriteItems(new HashSet<>(Set.of(item)));

        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.of(entity));

        assertDoesNotThrow(() -> favoriteService.delete(productId, userId));
        assertTrue(entity.getFavoriteItems().isEmpty());
        verify(favoriteRepository).touchFavoriteList(entity.getId());
    }

    @Test
    @DisplayName("delete is a no-op when no favorite list exists")
    void deleteNoOpWhenListAbsent() {
        UUID userId = UUID.randomUUID();
        when(favoriteRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> favoriteService.delete(UUID.randomUUID(), userId));
        verify(favoriteRepository, never()).save(any());
    }

    private static FavoriteListEntity favoriteList(UUID userId) {
        return FavoriteListEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();
    }

    private static FavoriteItemEntity favoriteItem(FavoriteListEntity favoriteList, UUID productId) {
        return FavoriteItemEntity.builder()
                .id(UUID.randomUUID())
                .favoriteListEntity(favoriteList)
                .productId(productId)
                .build();
    }

    private static ProductSnapshot productSnapshot(UUID id) {
        return new ProductSnapshot(
                id,
                "Coffee",
                "Desc",
                BigDecimal.TEN,
                100,
                true,
                null,
                BigDecimal.valueOf(4.5),
                12,
                "Brand",
                "Seller",
                250);
    }
}
