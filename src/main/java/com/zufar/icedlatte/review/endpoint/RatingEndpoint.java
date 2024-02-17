package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.AddNewMarkToProductRequest;
import com.zufar.icedlatte.openapi.dto.RatingDto;
import com.zufar.icedlatte.openapi.dto.RatingMark;
import com.zufar.icedlatte.openapi.order.api.RatingApi;
import com.zufar.icedlatte.review.api.RatingProvider;
import com.zufar.icedlatte.review.api.RatingUpdater;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = RatingEndpoint.RATING_URL)
public class RatingEndpoint implements RatingApi {

    public static final String RATING_URL = "/api/v1/rating/product";

    private final RatingUpdater ratingUpdater;
    private final RatingProvider ratingProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    @PostMapping
    public ResponseEntity<RatingDto> addNewMarkToProduct(@RequestBody final AddNewMarkToProductRequest rating) {
        log.info("Received the request to add new mark to product");
        final UUID userId = securityPrincipalProvider.getUserId();
        final RatingDto ratingDto = ratingUpdater.addRating(rating, userId);
        log.info("Mark was added");
        return ResponseEntity.ok().body(ratingDto);
    }

    @Override
    @GetMapping(value = "/{productId}")
    public ResponseEntity<List<RatingMark>> getRatingByProductId(@PathVariable final UUID productId) {
        log.info("Received the request to get rating by product id");
        final List<RatingMark> ratingMarks = ratingProvider.getRatingByProductId(productId);
        log.info("Rating by product id retrieval processed");
        return ResponseEntity.ok().body(ratingMarks);
    }
}
