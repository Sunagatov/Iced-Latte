package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.product.api.ProductApi;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddProductToFavorite {

    private final UserRepository userRepository;
    private final ProductApi productApi;
    private final UserApi userApi;
    private final UserDtoConverter userDtoConverter;

    public UserDto addNewItemToFavorite(final UUID productId, final UUID userId) {
        //todo
        //add product to favorite and save it
        return null;
    }
}
