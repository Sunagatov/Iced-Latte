package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.entity.FavoriteList;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GetFavoriteList {

    private final FavoriteRepository favoriteRepository;
    private final SingleUserProvider singleUserProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public FavoriteList getEntityFavoriteList(final UUID userId) {
        FavoriteList favoriteList = favoriteRepository.findByUserId(userId)
                .orElseGet(() -> createNewFavoriteList(userId));
        return favoriteList;
    }

    private FavoriteList createNewFavoriteList(UUID userId) {
        return FavoriteList.builder()
                .user(singleUserProvider.getUserEntityById(userId))
                .favoriteItems(ConcurrentHashMap.newKeySet())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
