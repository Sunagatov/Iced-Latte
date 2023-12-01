package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteProductsFromFavoriteList {

    private final GetFavoriteList getFavoriteList;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productId, final UUID userId) {
        FavoriteList favoriteList = getFavoriteList.getEntityFavoriteList(userId);
        favoriteList.getFavoriteItems().removeIf(item -> item.getProductInfo().getProductId().equals(productId));
    }
}
