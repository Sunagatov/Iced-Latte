package com.zufar.icedlatte.favorite.api;


import com.zufar.icedlatte.openapi.dto.UserDto;

import java.util.UUID;

public interface FavoriteApi {

    UserDto addNewItemToFavorite(UUID productId, UUID userId);
}
