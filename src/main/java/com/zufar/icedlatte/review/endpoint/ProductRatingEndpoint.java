package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.AddNewProductRatingRequest;
import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.review.api.RatingUpdater;
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
@RequestMapping(value = ProductRatingEndpoint.RATING_URL)
public class ProductRatingEndpoint {

    public static final String RATING_URL = "/api/v1/rating/product";

    private final RatingUpdater ratingUpdater;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @PostMapping
    public ResponseEntity<ProductRatingDto> addNewProductRating(@RequestBody final AddNewProductRatingRequest rating) {
        log.info("Received the request to add product's rating");
        final UUID userId = securityPrincipalProvider.getUserId();
        final ProductRatingDto ratingDto = ratingUpdater.addRating(rating, userId);
        log.info("New Product's Rating was added");
        return ResponseEntity.ok().body(ratingDto);
    }
}
