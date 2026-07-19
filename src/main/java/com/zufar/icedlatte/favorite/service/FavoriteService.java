package com.zufar.icedlatte.favorite.service;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.exception.FavoriteProductNotFoundException;
import com.zufar.icedlatte.favorite.exception.InvalidFavoriteRequestException;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final int MAX_FAVORITE_PRODUCT_IDS = 100;
    private static final int MAX_TOTAL_FAVORITE_PRODUCTS = 100;

    private final FavoriteRepository favoriteRepository;
    private final ProductCatalogApi productCatalogApi;

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ListOfFavoriteProductsDto getEnrichedFavoriteList(final UUID userId) {
        return favoriteRepository
                .findByUserId(userId)
                .map(this::toEnrichedDto)
                .orElseGet(() -> FavoriteListDtoConverter.toDto(List.of()));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ListOfFavoriteProductsDto add(final ListOfFavoriteProducts request, final UUID userId) {
        List<UUID> requestedProductIds = validateAndCopyProductIds(request);
        FavoriteListEntity favoriteList = getOrCreateFavoriteList(userId);

        Set<UUID> existingProductIds = extractProductIds(favoriteList);
        Set<UUID> newProductIds = requestedProductIds.stream()
                .filter(id -> !existingProductIds.contains(id))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        validateTotalFavoriteLimit(existingProductIds.size(), newProductIds.size());
        validateProductsExist(newProductIds);
        insertFavoriteItems(favoriteList.getId(), newProductIds);

        FavoriteListEntity refreshed = favoriteRepository
                .findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Favorite list not found for userId=" + userId));
        return toEnrichedDto(refreshed);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productId, final UUID userId) {
        favoriteRepository.findByUserId(userId).ifPresent(favoriteList -> {
            boolean removed = favoriteList
                    .getFavoriteItems()
                    .removeIf(item -> item.getProductId().equals(productId));
            if (removed) {
                favoriteRepository.touchFavoriteList(favoriteList.getId());
            }
        });
    }

    private FavoriteListEntity getOrCreateFavoriteList(UUID userId) {
        return favoriteRepository.findByUserId(userId).orElseGet(() -> {
            favoriteRepository.insertFavoriteListIfAbsent(UUID.randomUUID(), userId);
            favoriteRepository.flush();
            return favoriteRepository
                    .findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Favorite list not found for userId=" + userId));
        });
    }

    private ListOfFavoriteProductsDto toEnrichedDto(FavoriteListEntity entity) {
        List<UUID> productIds = extractSortedProductIds(entity);
        if (productIds.isEmpty()) {
            return FavoriteListDtoConverter.toDto(List.of());
        }
        List<ProductSnapshot> existingProducts = productCatalogApi.getProductsByIds(productIds);
        return FavoriteListDtoConverter.toDto(existingProducts);
    }

    private void validateProductsExist(Set<UUID> productIds) {
        if (productIds.isEmpty()) {
            return;
        }
        Set<UUID> foundIds = productCatalogApi.findExistingProductIds(productIds);
        List<UUID> missingIds =
                productIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new FavoriteProductNotFoundException(missingIds);
        }
    }

    private static void validateTotalFavoriteLimit(int existingProductCount, int newProductCount) {
        int totalProductCount = existingProductCount + newProductCount;
        if (totalProductCount > MAX_TOTAL_FAVORITE_PRODUCTS) {
            throw new InvalidFavoriteRequestException(
                    "Favorite list must contain no more than " + MAX_TOTAL_FAVORITE_PRODUCTS + " products.");
        }
    }

    private void insertFavoriteItems(UUID favoriteListId, Set<UUID> productIds) {
        if (productIds.isEmpty()) {
            return;
        }
        productIds.forEach(productId ->
                favoriteRepository.insertFavoriteItemIfAbsent(UUID.randomUUID(), favoriteListId, productId));
        favoriteRepository.touchFavoriteList(favoriteListId);
        favoriteRepository.flush();
    }

    private Set<UUID> extractProductIds(FavoriteListEntity entity) {
        return entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .collect(Collectors.toSet());
    }

    private List<UUID> extractSortedProductIds(FavoriteListEntity entity) {
        return entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .distinct()
                .sorted(comparing(UUID::toString))
                .toList();
    }

    private static List<UUID> validateAndCopyProductIds(ListOfFavoriteProducts request) {
        if (request.getProductIds() == null) {
            throw new InvalidFavoriteRequestException("Favorite request product ids must not be null.");
        }
        List<? extends @Nullable UUID> productIds = request.getProductIds();
        int productCount = productIds.size();
        if (productCount == 0) {
            throw new InvalidFavoriteRequestException("Favorite request must contain at least one product id.");
        }
        if (productCount > MAX_FAVORITE_PRODUCT_IDS) {
            throw new InvalidFavoriteRequestException(
                    "Favorite request must contain no more than " + MAX_FAVORITE_PRODUCT_IDS + " product ids.");
        }
        List<UUID> validatedProductIds = new ArrayList<>(productCount);
        for (@Nullable UUID productId : productIds) {
            if (productId == null) {
                throw new InvalidFavoriteRequestException("Favorite request product ids must not contain null values.");
            }
            validatedProductIds.add(productId);
        }
        return List.copyOf(validatedProductIds);
    }
}
