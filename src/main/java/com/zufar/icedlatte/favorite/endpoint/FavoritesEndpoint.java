package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.favorite.api.*;
import com.zufar.icedlatte.favorite.converter.*;
import com.zufar.icedlatte.favorite.dto.*;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.openapi.favorite.api.FavoriteProductsApi;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.validation.Valid;
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
    public ResponseEntity<ListOfFavoriteProductsDto> addListOfFavoriteProducts(@Validated @Valid @RequestBody final ListOfFavoriteProducts request) {
        log.info("Adding {} products to favorites", request.getProductIds().size());
        var userId = securityPrincipalProvider.getUserId();
        var favoriteList = favoriteProductAdderHelper.add(request, userId);
        var response = listOfFavoriteProductsDtoConverter.toListProductDto(favoriteList);
        log.info("Added {} products to favorites for user: {}", request.getProductIds().size(), userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<ListOfFavoriteProductsDto> getListOfFavoriteProducts() {
        log.info("Getting favorite products");
        var userId = securityPrincipalProvider.getUserId();
        var favoriteList = favoriteListProvider.getFavoriteListDto(userId);
        var response = listOfFavoriteProductsDtoConverter.toListProductDto(favoriteList);
        log.info("Retrieved {} favorite products for user: {}", response.getProducts().size(), userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping(value = "/{productId}")
    public ResponseEntity<Void> removeProductFromFavorite(@PathVariable final UUID productId) {
        // Validate UUID input to prevent code injection
        if (productId == null) {
            log.warn("Invalid product ID: null value received");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Removing product from favorites: {}", productId);
        var userId = securityPrincipalProvider.getUserId();
        favoriteProductDeleter.delete(productId, userId);
        log.info("Product removed from favorites: {} for user: {}", productId, userId);
        return ResponseEntity.ok().build();
    }
}
