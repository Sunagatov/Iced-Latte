package com.zufar.icedlatte.favorite.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddProductToFavorite {

    private final UserApi userApi;
    public UserDto addNewItemToFavorite(final UUID productId, final UUID userId) {
        return userApi.addNewItemToFavorite(productId, userId);
    }
}
