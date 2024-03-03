package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.ProductRating;
import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.openapi.product.rating.api.ProductRatingApi;
import com.zufar.icedlatte.review.api.RatingProvider;
import com.zufar.icedlatte.review.api.RatingUpdater;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = ProductRatingEndpoint.RATING_URL)
public class ProductRatingEndpoint implements ProductRatingApi {

    public static final String RATING_URL = "/api/v1/products/";

    private final RatingUpdater ratingUpdater;
    private final RatingProvider ratingProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    @PostMapping("/{productId}/ratings/{rating}")
    public ResponseEntity<ProductRatingDto> addNewProductRating(@PathVariable final UUID productId, @PathVariable @Max(5) @Min(1) final Integer rating) {
        log.info("Received the request to add new rating to product");
        final UUID userId = securityPrincipalProvider.getUserId();
        final ProductRatingDto ratingDto = ratingUpdater.addRating(userId, productId, rating);
        log.info("Rating was added");
        return ResponseEntity.ok().body(ratingDto);
    }

    @Override
    @GetMapping(value = "{productId}/ratings")
    public ResponseEntity<List<ProductRating>> getRatingByProductId(@PathVariable final UUID productId) {
        log.info("Received the request to get rating by product id");
        final List<ProductRating> ratingMarks = ratingProvider.getRatingByProductId(productId);
        log.info("Rating by product id retrieval processed");
        return ResponseEntity.ok().body(ratingMarks);
    }
}
