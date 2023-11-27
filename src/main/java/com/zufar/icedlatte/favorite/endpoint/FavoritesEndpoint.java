package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.favorite.api.AddProductsToFavoriteList;
import com.zufar.icedlatte.favorite.converter.ListOfFavoriteProductsDtoConverter;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.favorite.api.FavoriteProductsApi;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = FavoritesEndpoint.FAVORITES_URL)
public class FavoritesEndpoint implements FavoriteProductsApi {

    public static final String FAVORITES_URL = "/api/v1/favorites";

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ListOfFavoriteProductsDtoConverter listOfFavoriteProductsDtoConverter;
    private final AddProductsToFavoriteList addProductsToFavoriteList;

    @Override
    @PostMapping
    public ResponseEntity<ListOfFavoriteProductsDto> addListOfFavoriteProducts(@RequestBody final ListOfFavoriteProducts request) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.warn("Received the request to add a list of favorite products for the user with id: {}", userId);
        FavoriteListDto favoriteList = addProductsToFavoriteList.add(request, userId);
        ListOfFavoriteProductsDto listOfFavoriteProductsDto = listOfFavoriteProductsDtoConverter.toListProductDto(favoriteList);
        log.info("The list of favorite products for the user with id: {} was added successfully", userId);
        return ResponseEntity.ok()
                .body(listOfFavoriteProductsDto);
    }
}