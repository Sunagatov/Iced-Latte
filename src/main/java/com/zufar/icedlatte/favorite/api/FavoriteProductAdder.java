package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.converter.ListOfFavoriteProductsDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteProductAdder {

    private final FavoriteRepository favoriteRepository;
    private final ProductInfoRepository productInfoRepository;
    private final FavoriteListDtoConverter favoriteListDtoConverter;
    private final ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;
    private final FavoriteListProvider favoriteListProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ListOfFavoriteProductsDto add(final ListOfFavoriteProducts listOfFavoriteProducts,
                                         final UUID userId) {
        FavoriteListEntity favoriteListEntity = favoriteListProvider.getFavoriteListEntity(userId);

        Set<UUID> existingFavoriteProductIds = extractFavoriteProductIds(favoriteListEntity);
        Set<UUID> newFavoriteItemIds = filterNewFavoriteProductIds(listOfFavoriteProducts, existingFavoriteProductIds);

        Set<FavoriteItemEntity> newFavoriteItems = createFavoriteItems(newFavoriteItemIds, favoriteListEntity);
        favoriteListEntity.getFavoriteItems().addAll(newFavoriteItems);

        FavoriteListEntity updatedFavoriteListEntity = saveWithConcurrentInsertRecovery(
                favoriteListEntity, newFavoriteItemIds, userId);
        FavoriteListDto dto = favoriteListDtoConverter.toDto(updatedFavoriteListEntity);
        ListOfFavoriteProductsDto response = listOfFavoriteProductsDtoConverter.toListProductDto(dto);
        productPictureLinkUpdater.updateBatch(response.getProducts());
        return response;
    }

    private FavoriteListEntity saveWithConcurrentInsertRecovery(FavoriteListEntity favoriteListEntity,
                                                                Set<UUID> newFavoriteItemIds,
                                                                UUID userId) {
        try {
            return favoriteRepository.save(favoriteListEntity);
        } catch (DataIntegrityViolationException ex) {
            if (!isFavoriteItemUniqueConstraintViolation(ex)) {
                throw ex;
            }
            log.warn("favourites.add.concurrent_conflict: userId={}", userId);
            FavoriteListEntity freshFavoriteList = favoriteListProvider.getFavoriteListEntity(userId);
            Set<UUID> existingFavoriteProductIds = extractFavoriteProductIds(freshFavoriteList);
            Set<UUID> stillMissingIds = newFavoriteItemIds.stream()
                    .filter(productId -> !existingFavoriteProductIds.contains(productId))
                    .collect(Collectors.toSet());
            freshFavoriteList.getFavoriteItems().addAll(createFavoriteItems(stillMissingIds, freshFavoriteList));
            return favoriteRepository.save(freshFavoriteList);
        }
    }

    private Set<UUID> extractFavoriteProductIds(FavoriteListEntity favoriteListEntity) {
        return favoriteListEntity.getFavoriteItems().stream()
                .map(item -> item.getProductInfo().getId())
                .collect(Collectors.toSet());
    }

    private Set<UUID> filterNewFavoriteProductIds(ListOfFavoriteProducts listOfFavoriteProducts,
                                                  Set<UUID> existingIds) {
        return listOfFavoriteProducts.getProductIds().stream()
                .filter(productId -> !existingIds.contains(productId))
                .collect(Collectors.toSet());
    }

    private Set<FavoriteItemEntity> createFavoriteItems(Set<UUID> productIds,
                                                        FavoriteListEntity favoriteListEntity) {
        List<ProductInfo> foundProducts = productInfoRepository.findAllById(productIds);
        Set<UUID> foundIds = foundProducts.stream()
                .map(ProductInfo::getId)
                .collect(Collectors.toSet());
        List<UUID> missingIds = productIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ProductNotFoundException(missingIds);
        }
        return foundProducts.stream()
                .map(productInfo -> FavoriteItemEntity.builder()
                        .favoriteListEntity(favoriteListEntity)
                        .productInfo(productInfo)
                        .build())
                .collect(Collectors.toSet());
    }

    private static boolean isFavoriteItemUniqueConstraintViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.contains("uq_favorite_item_list_product");
    }
}
