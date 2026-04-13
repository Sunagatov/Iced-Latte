package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.favorite.api.FavoriteListProvider;
import com.zufar.icedlatte.favorite.api.FavoriteProductAdder;
import com.zufar.icedlatte.favorite.api.FavoriteProductDeleter;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProducts;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.favorite.api.FavoriteProductsApi;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final FavoriteProductAdder favoriteProductAdderHelper;
    private final FavoriteListProvider favoriteListProvider;
    private final FavoriteProductDeleter favoriteProductDeleter;

    @Override
    @PostMapping
    public ResponseEntity<ListOfFavoriteProductsDto> addListOfFavoriteProducts(@Validated @Valid @RequestBody final ListOfFavoriteProducts request) {
        var userId = securityPrincipalProvider.getUserId();
        var response = favoriteProductAdderHelper.add(request, userId);
        log.info("favourites.added: count={}", request.getProductIds().size());
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<ListOfFavoriteProductsDto> getListOfFavoriteProducts() {
        var userId = securityPrincipalProvider.getUserId();
        var response = favoriteListProvider.getEnrichedFavoriteList(userId);
        log.debug("favourites.retrieved: count={}, userId={}", response.getProducts().size(), userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping(value = "/{productId}")
    public ResponseEntity<Void> removeProductFromFavorite(@PathVariable final UUID productId) {
        if (productId == null) {
            log.warn("favourites.remove.invalid: reason=null_productId");
            return ResponseEntity.badRequest().build();
        }
        var userId = securityPrincipalProvider.getUserId();
        favoriteProductDeleter.delete(productId, userId);
        log.info("favourites.removed: productId={}", productId);
        return ResponseEntity.ok().build();
    }
}
