package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.favorite.converter.FavoriteListDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFavoritesProductsByUserId {

    private final GetFavoriteList getFavoriteList;
    private final FavoriteListDtoConverter favoriteListDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public FavoriteListDto get(final UUID userId) {
        FavoriteList favoriteList = getFavoriteList.getEntityFavoriteList(userId);
        FavoriteListDto favoriteListDto = favoriteListDtoConverter.toDto(favoriteList);
        return favoriteListDto;
    }
}
