package com.zufar.icedlatte.review.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.user.api.UserDeletionRequestedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserReviewCleanup {
    private final ProductReviewManager reviewManager;

    @EventListener
    public void beforeUserDeletion(UserDeletionRequestedEvent event) {
        reviewManager.removeUserReviewsAndVotes(event.userId());
    }
}
