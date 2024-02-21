package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.review.repository.ReviewRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewDeleter {

    private final ReviewRepository reviewRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductReviewProvider productReviewProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productReviewId) {
        var review = productReviewProvider.getReviewEntityById(productReviewId);
        var deleterId = securityPrincipalProvider.getUserId();
        var creatorId = review.getUserId();
        if (!deleterId.equals(creatorId)) {
            log.warn("Failed to delete product review: {}", productReviewId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        reviewRepository.deleteById(productReviewId);
    }
}
