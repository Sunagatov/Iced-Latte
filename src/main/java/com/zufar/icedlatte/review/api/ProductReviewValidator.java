package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.product.api.SingleProductProvider;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewValidator {

    private final SingleProductProvider singleProductProvider;

    public void validateReview(final UUID productId, final String text) {
        if (text.isEmpty()) {
            throw new EmptyProductReviewException();
        }
        checkProduct(productId);
    }

    public void checkProduct(final UUID productId) {
        // check if product exists
        singleProductProvider.getProductEntityById(productId);
    }
}
