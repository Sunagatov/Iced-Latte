package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.NewReview;
import com.zufar.icedlatte.openapi.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewManager {

    private final ReviewCreator reviewCreator;

    public ReviewResponse createReview(final UUID productId, final NewReview newReview) {
        return reviewCreator.create(productId, newReview);
    }
}
