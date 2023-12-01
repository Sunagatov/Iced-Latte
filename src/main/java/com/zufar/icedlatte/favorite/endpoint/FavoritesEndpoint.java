package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.favorite.api.FavoriteListProvider;
import com.zufar.icedlatte.favorite.api.FavoriteProductAdder;
import com.zufar.icedlatte.favorite.api.FavoriteProductDeleter;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

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
    private final FavoriteProductAdder favoriteProductAdderHelper;
    private final FavoriteListProvider favoriteListProvider;
    private final FavoriteProductDeleter favoriteProductDeleter;

    @Override
    @PostMapping
    public ResponseEntity<ListOfFavoriteProductsDto> addListOfFavoriteProducts(@RequestBody final ListOfFavoriteProducts request) {
        log.info("Received the request to add a list of favorite products.");
        UUID userId = securityPrincipalProvider.getUserId();
        FavoriteListDto favoriteList = favoriteProductAdderHelper.add(request, userId);
        ListOfFavoriteProductsDto listOfFavoriteProductsDto = listOfFavoriteProductsDtoConverter.toListProductDto(favoriteList);
        log.info("Favorite products addition processed.");
        return ResponseEntity.ok().body(listOfFavoriteProductsDto);
    }

    @Override
    @GetMapping
    public ResponseEntity<ListOfFavoriteProductsDto> getListOfFavoriteProducts() {
        log.info("Received the request to retrieve the list of favorite products.");
        UUID userId = securityPrincipalProvider.getUserId();
        FavoriteListDto favoriteList = favoriteListProvider.getFavoriteListDto(userId);
        ListOfFavoriteProductsDto listOfFavoriteProductsDto = listOfFavoriteProductsDtoConverter.toListProductDto(favoriteList);
        log.info("Favorite products retrieval processed.");
        return ResponseEntity.ok().body(listOfFavoriteProductsDto);
    }

    @Override
    @DeleteMapping(value = "/{productId}")
    public ResponseEntity<Void> removeProductFromFavorite(@PathVariable final UUID productId) {
        log.info("Received the request to delete a product from the favorite list.");
        UUID userId = securityPrincipalProvider.getUserId();
        favoriteProductDeleter.delete(productId, userId);
        log.info("Product removal from favorite list processed.");
        return ResponseEntity.ok().build();
    }
}
