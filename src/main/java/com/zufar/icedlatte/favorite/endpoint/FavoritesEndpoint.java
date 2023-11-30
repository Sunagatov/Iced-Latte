package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.favorite.api.AddProductsToFavoriteList;
import com.zufar.icedlatte.favorite.api.DeleteProductsFromFavoriteList;
import com.zufar.icedlatte.favorite.api.GetFavoritesProductsByUserId;
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
    private final AddProductsToFavoriteList addProductsToFavoriteListHelper;
    private final GetFavoritesProductsByUserId getFavoritesProductsByUserId;
    private final DeleteProductsFromFavoriteList deleteProductsFromFavoriteListHelper;

    @Override
    @PostMapping
    public ResponseEntity<ListOfFavoriteProductsDto> addListOfFavoriteProducts(@RequestBody final ListOfFavoriteProducts request) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.warn("Received the request to add a list of favorite products for the user with id: {}", userId);
        FavoriteListDto favoriteList = addProductsToFavoriteListHelper.add(request, userId);
        ListOfFavoriteProductsDto listOfFavoriteProductsDto = listOfFavoriteProductsDtoConverter.toListProductDto(favoriteList);
        log.info("The list of favorite products for the user with id: {} was added successfully", userId);
        return ResponseEntity.ok()
                .body(listOfFavoriteProductsDto);
    }

    @Override
    @GetMapping
    public ResponseEntity<ListOfFavoriteProductsDto> getListOfFavoriteProducts() {
        UUID userId = securityPrincipalProvider.getUserId();
        log.warn("Received the request to get a list of favorite products for the user with id: {}", userId);
        FavoriteListDto favoriteList = getFavoritesProductsByUserId.get(userId);
        ListOfFavoriteProductsDto listOfFavoriteProductsDto = listOfFavoriteProductsDtoConverter.toListProductDto(favoriteList);
        log.info("The list of favorite products for the user with id: {} was retrieved successfully", userId);
        return ResponseEntity.ok()
                .body(listOfFavoriteProductsDto);
    }

    @Override
    @DeleteMapping(value = "/{productId}")
    public ResponseEntity<Void> removeProductFromFavorite(@PathVariable final UUID productId) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.warn("Received the request to delete a product from favorite list for the user with id: {}", userId);
        deleteProductsFromFavoriteListHelper.delete(productId, userId);
        log.info("The product of favorite list for the user with id: {} was deleted successfully", userId);
        return ResponseEntity.ok()
                .build();
    }
}