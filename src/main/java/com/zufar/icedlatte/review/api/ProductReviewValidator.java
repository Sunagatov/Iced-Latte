package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.product.api.SingleProductProvider;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewValidator {

    private final SingleProductProvider singleProductProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public void validate(final UUID productId, final String text) {
        if (text.isEmpty()) {
            throw new EmptyProductReviewException();
        }
        // check if product exists
        singleProductProvider.getProductEntityById(productId);
    }
}
