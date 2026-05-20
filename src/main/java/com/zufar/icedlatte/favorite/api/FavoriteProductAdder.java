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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteProductAdder {

    private final FavoriteRepository favoriteRepository;
    private final ProductCatalogApi productCatalogApi;
    private final FavoriteListDtoConverter favoriteListDtoConverter;
    private final ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;
    private final FavoriteListProvider favoriteListProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ListOfFavoriteProductsDto add(final ListOfFavoriteProducts listOfFavoriteProducts,
                                         final UUID userId) {
        FavoriteListEntity favoriteListEntity = favoriteListProvider.getFavoriteListEntity(userId);

        Set<UUID> existingProductIds = extractFavoriteProductIds(favoriteListEntity);
        Set<UUID> newProductIds = filterNewFavoriteProductIds(listOfFavoriteProducts, existingProductIds);

        validateProductsExist(newProductIds);

        Set<FavoriteItemEntity> newFavoriteItems = newProductIds.stream()
                .map(productId -> FavoriteItemEntity.builder()
                        .favoriteListEntity(favoriteListEntity)
                        .productId(productId)
                        .build())
                .collect(Collectors.toSet());
        favoriteListEntity.getFavoriteItems().addAll(newFavoriteItems);

        FavoriteListEntity updatedEntity = saveWithConcurrentInsertRecovery(
                favoriteListEntity, newProductIds, userId);
        return toEnrichedDto(updatedEntity);
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

    private FavoriteListEntity saveWithConcurrentInsertRecovery(FavoriteListEntity favoriteListEntity,
                                                                Set<UUID> newProductIds,
                                                                UUID userId) {
        try {
            return favoriteRepository.save(favoriteListEntity);
        } catch (DataIntegrityViolationException ex) {
            if (!isFavoriteItemUniqueConstraintViolation(ex)) {
                throw ex;
            }
            log.warn("favourites.add.concurrent_conflict: userId={}", userId);
            FavoriteListEntity freshFavoriteList = favoriteListProvider.getFavoriteListEntity(userId);
            Set<UUID> existingProductIds = extractFavoriteProductIds(freshFavoriteList);
            Set<UUID> stillMissingIds = newProductIds.stream()
                    .filter(productId -> !existingProductIds.contains(productId))
                    .collect(Collectors.toSet());
            stillMissingIds.forEach(productId ->
                    freshFavoriteList.getFavoriteItems().add(FavoriteItemEntity.builder()
                            .favoriteListEntity(freshFavoriteList)
                            .productId(productId)
                            .build()));
            return favoriteRepository.save(freshFavoriteList);
        }
    }

    private Set<UUID> extractFavoriteProductIds(FavoriteListEntity favoriteListEntity) {
        return favoriteListEntity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .collect(Collectors.toSet());
    }

    private Set<UUID> filterNewFavoriteProductIds(ListOfFavoriteProducts listOfFavoriteProducts,
                                                  Set<UUID> existingIds) {
        return listOfFavoriteProducts.getProductIds().stream()
                .filter(productId -> !existingIds.contains(productId))
                .collect(Collectors.toSet());
    }

    private static boolean isFavoriteItemUniqueConstraintViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.contains("uq_favorite_item_list_product");
    }
}
