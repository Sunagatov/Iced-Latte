package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.excaption.FavoritesPageException;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteListPageProvider {

    private final ProductInfoDtoConverter productInfoDtoConverter;
    private final FavoriteRepository favoriteRepository;

    private final Integer PAGE_SIZE = 2;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public List<ProductInfoDto> getFavoritesProductsByPage(final UUID userId, final Integer page) {
        final Pageable pageable = Pageable.ofSize(PAGE_SIZE).withPage(page + 1);
        List<FavoriteItemEntity> favoriteItemEntities = favoriteRepository.findFavoriteItemsByUserIdWithPagination(userId, pageable);
        listItemsValidation(favoriteItemEntities, userId);
        List<ProductInfoDto> products = favoriteItemEntities.stream()
                .map(FavoriteItemEntity::getProductInfo)
                .map(productInfoDtoConverter::toDto)
                .collect(Collectors.toList());
        return products;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Integer getNumberOfPages(final UUID userId) {
        Integer totalItems = favoriteRepository.getPageQuantity(userId);
        return (int) Math.ceil((double) totalItems / PAGE_SIZE);
    }


    private void listItemsValidation(List<FavoriteItemEntity> favoriteItems, UUID userId) {
        if (favoriteItems == null || favoriteItems.isEmpty()) {
            throw new FavoritesPageException(userId);
        }
    }
}