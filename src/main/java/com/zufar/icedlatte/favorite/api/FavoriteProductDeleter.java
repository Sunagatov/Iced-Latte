package com.zufar.icedlatte.favorite.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteProductDeleter {

    private final FavoriteListProvider favoriteListProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productId,
                       final UUID userId) {
        favoriteListProvider.findFavoriteListEntity(userId).ifPresent(favoriteList ->
                favoriteList.getFavoriteItems()
                        .removeIf(item -> item.getProductId().equals(productId))
        );
    }
}
