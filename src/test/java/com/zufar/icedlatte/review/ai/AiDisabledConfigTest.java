package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.review.ai.moderation.ReviewModerationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AiDisabledConfig")
class AiDisabledConfigTest {

    private final AiDisabledConfig config = new AiDisabledConfig();

    @Test
    @DisplayName("provides no-op moderation service")
    void noOpModerationService_doesNothing() {
        ReviewModerationService service = config.noOpModerationService();

        assertThatCode(() -> service.moderate("any review text"))
                .doesNotThrowAnyException();
    }
}
