package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@DisplayName("Avatar upload AWS configuration")
class AvatarUploadAwsConfigurationTest {

    @Test
    @DisplayName("presigner is active only when AWS is enabled and avatar mode is presigned")
    void presignerIsActiveOnlyForPresignedAwsMode() {
        assertConditionalOnProperty(AwsAvatarUploadPresigner.class, "spring.aws.enabled", "true");
        assertConditionalOnProperty(AwsAvatarUploadPresigner.class, "avatar.upload-mode", "presigned");
    }

    @Test
    @DisplayName("completion poller is active only for presigned AWS queue processing")
    void completionPollerIsActiveOnlyForPresignedAwsQueueProcessing() {
        assertConditionalOnProperty(AvatarUploadCompletionQueuePoller.class, "avatar.completion-queue.enabled", "true");
        assertConditionalOnProperty(AvatarUploadCompletionQueuePoller.class, "spring.aws.enabled", "true");
        assertConditionalOnProperty(AvatarUploadCompletionQueuePoller.class, "avatar.upload-mode", "presigned");
    }

    private void assertConditionalOnProperty(Class<?> type, String name, String havingValue) {
        ConditionalOnProperty[] conditions = type.getAnnotationsByType(ConditionalOnProperty.class);

        assertThat(Arrays.stream(conditions)
                        .filter(condition -> Arrays.asList(condition.name()).contains(name))
                        .findFirst())
                .isPresent()
                .get()
                .satisfies(condition -> {
                    assertThat(condition.havingValue()).isEqualTo(havingValue);
                    assertThat(condition.matchIfMissing()).isFalse();
                });
    }
}
