package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.AddNewMarkToProductRequest;
import com.zufar.icedlatte.openapi.dto.RatingDto;
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
@RequestMapping(value = RatingEndpoint.RATING_URL)
public class RatingEndpoint {

    public static final String RATING_URL = "/api/v1/products/rating";

    private final RatingUpdater ratingUpdater;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @PostMapping
    public ResponseEntity<RatingDto> addNewMarkToProduct(@RequestBody final AddNewMarkToProductRequest rating) {
        log.info("Received the request to add new mark to product");
        final UUID userId = securityPrincipalProvider.getUserId();
        final RatingDto ratingDto = ratingUpdater.addRating(rating, userId);
        log.info("Mark was added");
        return ResponseEntity.ok().body(ratingDto);
    }
}
