package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService implements FavoriteApi {

    private final AddProductToFavorite addProductToFavorite;
    @Override
    public UserDto addNewItemToFavorite(UUID productId, UUID userId) {
        return addProductToFavorite.addNewItemToFavorite(productId, userId);
    }
}
