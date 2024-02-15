package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.RatingMark;
import com.zufar.icedlatte.review.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingProvider {

    private final RatingRepository ratingRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public List<RatingMark> getRatingByProductId(final UUID productId) {
        List<Object[]> resultList = ratingRepository.countRatingMarksByProductId(productId);
        List<RatingMark> ratingMarks = new ArrayList<>();

        for (Object[] result : resultList) {
            Integer mark = ((Number) result[0]).intValue();
            Integer quantity = ((Number) result[1]).intValue();
            ratingMarks.add(new RatingMark(mark, quantity));
        }

        return ratingMarks;
    }
}
