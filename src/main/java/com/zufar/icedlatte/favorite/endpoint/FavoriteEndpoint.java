package com.zufar.icedlatte.favorite.endpoint;

import com.zufar.icedlatte.openapi.favorite.api.FavoriteProductsApi;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = FavoriteEndpoint.FAVORITE_URL)
public class FavoriteEndpoint implements FavoriteProductsApi {

    public static final String FAVORITE_URL = "/api/v1/favorite";
    private final SecurityPrincipalProvider securityPrincipalProvider;

}
