package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItem;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddProductsToFavoriteList {

    private final FavoriteRepository favoriteRepository;
    private final SingleUserProvider singleUserProvider;
    private final ProductInfoRepository productInfoRepository;
    private final FavoriteListDtoConverter favoriteListDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public FavoriteListDto add(final ListOfFavoriteProducts listOfFavoriteProducts, final UUID userId) {
        FavoriteList favoriteList = favoriteRepository.findByUserId(userId)
                .orElseGet(() -> createNewFavoriteList(userId));

        Set<FavoriteItem> favoriteItems = createFavoriteItems(listOfFavoriteProducts, favoriteList);
        favoriteList.getFavoriteItems().addAll(favoriteItems);

        FavoriteList updatedFavoriteList = favoriteRepository.save(favoriteList);
        return favoriteListDtoConverter.toDto(updatedFavoriteList);
    }

    private Set<FavoriteItem> createFavoriteItems(final ListOfFavoriteProducts listOfFavoriteProducts, final FavoriteList favoriteList) {
        Set<UUID> favoriteItemIds = favoriteList.getFavoriteItems().stream()
                .map(item -> item.getProductInfo().getProductId())
                .collect(Collectors.toSet());

        Set<UUID> newFavoriteItemIds = listOfFavoriteProducts.getProductIds().stream()
                .filter(productId -> !favoriteItemIds.contains(productId))
                .collect(Collectors.toSet());

        return productInfoRepository.findAllById(newFavoriteItemIds).stream()
                .map(productInfo -> FavoriteItem.builder()
                        .favoriteList(favoriteList)
                        .productInfo(productInfo)
                        .build())
                .collect(Collectors.toSet());
    }

    private FavoriteList createNewFavoriteList(UUID userId) {
        return FavoriteList.builder()
                .user(singleUserProvider.getUserEntityById(userId))
                .favoriteItems(ConcurrentHashMap.newKeySet())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}