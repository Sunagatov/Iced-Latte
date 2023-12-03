package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.favorite.entity.FavoriteItem;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
import com.zufar.icedlatte.favorite.repository.FavoriteRepository;
import com.zufar.icedlatte.openapi.favorite.api.FavoriteProductsApi;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = FavoritesEndpoint.FAVORITES_URL)
public class FavoritesEndpoint implements FavoriteProductsApi {

    public static final String FAVORITES_URL = "/api/v1/favorites";
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final FavoriteRepository favoriteRepository;

    @GetMapping
    public List<ProductInfo> getFavoriteList(){
        UUID userId = securityPrincipalProvider.getUserId();
        FavoriteList favoriteList = favoriteRepository.getReferenceById(userId);
        return favoriteList.getFavoriteItems()
                .stream()
                .map(FavoriteItem::getProductInfo)
                .toList();
    }

    @DeleteMapping(value = "/{productId}")
    public List<ProductInfo> deleteFavoriteProduct(@PathVariable final UUID productId){
        UUID userId = securityPrincipalProvider.getUserId();
        FavoriteList favoriteList = favoriteRepository.getReferenceById(userId);
        if(!favoriteList.containsFavoriteItem(productId)){
            throw new NoSuchElementException("There's no product with ID " + productId + " on the list.");
        }
        favoriteList.removeFavoriteItemById(productId);
        return favoriteList.getFavoriteItems()
                .stream()
                .map(FavoriteItem::getProductInfo)
                .toList();
    }

}