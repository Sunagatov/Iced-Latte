package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.converter.ListOfFavoriteProductsDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteListProvider {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteListDtoConverter favoriteListDtoConverter;
    private final ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    private final ProductCatalogApi productCatalogApi;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public FavoriteListEntity getFavoriteListEntity(final UUID userId) {
        return favoriteRepository.findByUserId(userId)
                .orElseGet(() -> createAndSaveFavoriteList(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public java.util.Optional<FavoriteListEntity> findFavoriteListEntity(final UUID userId) {
        return favoriteRepository.findByUserId(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ListOfFavoriteProductsDto getEnrichedFavoriteList(final UUID userId) {
        FavoriteListEntity entity = favoriteRepository.findByUserId(userId)
                .orElseGet(() -> createNewFavoriteList(userId));

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
}
