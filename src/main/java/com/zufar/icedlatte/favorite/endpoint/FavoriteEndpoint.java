package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.favorite.api.FavoriteApi;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = FavoriteEndpoint.API_CUSTOMERS)
public class FavoriteEndpoint {

    public static final String API_CUSTOMERS = "/api/v1/favorites";

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final FavoriteApi favoriteApi;


    @PostMapping(value = "/{productId}")
    public ResponseEntity<Void> addNewItemToFavorite(@PathVariable final UUID productId) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.info("Received the request to add a new item to the favorite for the user with id: {}", userId);
        favoriteApi.addNewItemToFavorite(productId, userId);

        return (ResponseEntity<Void>) ResponseEntity.ok();
    }
}