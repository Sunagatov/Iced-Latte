package com.zufar.icedlatte.favorite.endpoint;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.favorite.service.FavoriteService;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.favorite.api.FavoriteProductsApi;
import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(FavoritesEndpoint.FAVORITES_URL)
public class FavoritesEndpoint implements FavoriteProductsApi {

    public static final String FAVORITES_URL = ApiPaths.FAVORITES;

    private final CurrentUserProvider currentUserProvider;
    private final FavoriteService favoriteService;

    @Override
    @PostMapping
    public ResponseEntity<ListOfFavoriteProductsDto> addListOfFavoriteProducts(
            @Valid @RequestBody final ListOfFavoriteProducts request) {
        var userId = currentUserProvider.getUserId();
        var response = favoriteService.add(request, userId);
        log.debug("favourites.added: count={}", request.getProductIds().size());
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<ListOfFavoriteProductsDto> getListOfFavoriteProducts() {
        var userId = currentUserProvider.getUserId();
        var response = favoriteService.getEnrichedFavoriteList(userId);
        log.debug(
                "favourites.retrieved: count={}, userId={}",
                response.getProducts().size(),
                userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeProductFromFavorite(@PathVariable final UUID productId) {
        var userId = currentUserProvider.getUserId();
        favoriteService.delete(productId, userId);
        log.debug("favourites.removed: productId={}", productId);
        return ResponseEntity.ok().build();
    }
}
