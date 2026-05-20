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
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteListDtoConverter favoriteListDtoConverter;
    private final ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    private final ProductCatalogApi productCatalogApi;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ListOfFavoriteProductsDto getEnrichedFavoriteList(final UUID userId) {
        FavoriteListEntity entity = favoriteRepository.findByUserId(userId)
                .orElseGet(() -> createNewFavoriteList(userId));
        return toEnrichedDto(entity);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ListOfFavoriteProductsDto add(final ListOfFavoriteProducts request, final UUID userId) {
        FavoriteListEntity favoriteList = getOrCreateFavoriteList(userId);

        Set<UUID> existingProductIds = extractProductIds(favoriteList);
        Set<UUID> newProductIds = request.getProductIds().stream()
                .filter(id -> !existingProductIds.contains(id))
                .collect(Collectors.toSet());

        validateProductsExist(newProductIds);

        newProductIds.forEach(productId ->
                favoriteList.getFavoriteItems().add(FavoriteItemEntity.builder()
                        .favoriteListEntity(favoriteList)
                        .productId(productId)
                        .build()));

        FavoriteListEntity saved = saveWithConcurrentInsertRecovery(favoriteList, newProductIds, userId);
        return toEnrichedDto(saved);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productId, final UUID userId) {
        favoriteRepository.findByUserId(userId).ifPresent(favoriteList ->
                favoriteList.getFavoriteItems()
                        .removeIf(item -> item.getProductId().equals(productId)));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private FavoriteListEntity getOrCreateFavoriteList(UUID userId) {
        return favoriteRepository.findByUserId(userId)
                .orElseGet(() -> createAndSaveFavoriteList(userId));
    }

    private ListOfFavoriteProductsDto toEnrichedDto(FavoriteListEntity entity) {
        List<UUID> productIds = entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .toList();
        Map<UUID, ProductInfoDto> productsById = productCatalogApi.getProductsByIds(productIds).stream()
                .collect(Collectors.toMap(ProductInfoDto::getId, Function.identity()));

        FavoriteListDto dto = favoriteListDtoConverter.toDto(entity, productsById);
        ListOfFavoriteProductsDto response = listOfFavoriteProductsDtoConverter.toListProductDto(dto);
        productPictureLinkUpdater.updateBatch(response.getProducts());
        return response;
    }

    private void validateProductsExist(Set<UUID> productIds) {
        if (productIds.isEmpty()) return;
        List<ProductInfoDto> found = productCatalogApi.getProductsByIds(productIds.stream().toList());
        Set<UUID> foundIds = found.stream().map(ProductInfoDto::getId).collect(Collectors.toSet());
        List<UUID> missingIds = productIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new ProductNotFoundException(missingIds);
        }
    }

    private FavoriteListEntity saveWithConcurrentInsertRecovery(FavoriteListEntity favoriteList,
                                                                Set<UUID> newProductIds,
                                                                UUID userId) {
        try {
            return favoriteRepository.save(favoriteList);
        } catch (DataIntegrityViolationException ex) {
            if (!isFavoriteItemUniqueConstraintViolation(ex)) {
                throw ex;
            }
            log.warn("favourites.add.concurrent_conflict: userId={}", userId);
            FavoriteListEntity fresh = getOrCreateFavoriteList(userId);
            Set<UUID> existingIds = extractProductIds(fresh);
            newProductIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .forEach(productId -> fresh.getFavoriteItems().add(FavoriteItemEntity.builder()
                            .favoriteListEntity(fresh)
                            .productId(productId)
                            .build()));
            return favoriteRepository.save(fresh);
        }
    }

    private FavoriteListEntity createAndSaveFavoriteList(UUID userId) {
        try {
            return favoriteRepository.save(createNewFavoriteList(userId));
        } catch (DataIntegrityViolationException ex) {
            log.warn("favourites.create.concurrent_conflict: userId={}", userId);
            return favoriteRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Favorite list not found after uniqueness conflict for userId=" + userId, ex));
        }
    }

    private FavoriteListEntity createNewFavoriteList(UUID userId) {
        return FavoriteListEntity.builder()
                .userId(userId)
                .favoriteItems(new HashSet<>())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private Set<UUID> extractProductIds(FavoriteListEntity entity) {
        return entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .collect(Collectors.toSet());
    }

    private static boolean isFavoriteItemUniqueConstraintViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.contains("uq_favorite_item_list_product");
    }
}
