package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.api.ProductReviewsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/users")
public class UserReviewEndpoint {

    private final ProductReviewsProvider productReviewsProvider;

    @GetMapping(value = "/reviews")
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getUserReviews(
            @RequestParam(name = "page", required = false) final Integer pageNumber,
            @RequestParam(name = "size", required = false) final Integer pageSize,
            @RequestParam(name = "sort_attribute", required = false) final String sortAttribute,
            @RequestParam(name = "sort_direction", required = false) final String sortDirection) {
        return ResponseEntity.ok(productReviewsProvider.getUserReviews(pageNumber, pageSize, sortAttribute, sortDirection));
    }
}
