package com.zufar.icedlatte.review.endpoint;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewLikeDto;
import com.zufar.icedlatte.openapi.product.review.api.ReviewReactionsApi;
import com.zufar.icedlatte.review.service.ProductReviewManager;
import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ReviewReactionsEndpoint implements ReviewReactionsApi {

    private final ProductReviewManager productReviewService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping(ApiPaths.PRODUCTS + "/{productId}/reviews/{productReviewId}/likes")
    public ResponseEntity<ProductReviewDto> addProductReviewLike(
            @PathVariable UUID productId,
            @PathVariable UUID productReviewId,
            @Valid @RequestBody ProductReviewLikeDto request) {
        UUID userId = currentUserProvider.getUserId();
        var productReview = productReviewService.updateLike(productId, productReviewId, userId, request.getIsLike());
        log.info(
                "review.rated: reviewId={}, vote={}",
                productReviewId,
                Boolean.TRUE.equals(request.getIsLike()) ? "liked" : "disliked");
        return ResponseEntity.ok(productReview);
    }
}
