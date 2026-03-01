package com.zufar.icedlatte.review.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LangChain4jReviewAiService unit tests")
class LangChain4jReviewAiServiceTest {

    @Mock
    private ReviewAiService reviewAiService;

    @InjectMocks
    private LangChain4jReviewAiService service;

    @Test
    @DisplayName("moderate: passes when AI returns OK")
    void moderate_ok_doesNotThrow() {
        when(reviewAiService.moderate("Great coffee!")).thenReturn("OK");
        service.moderate("Great coffee!");
    }

    @Test
    @DisplayName("moderate: throws ReviewModerationException when AI returns NOT_OK")
    void moderate_notOk_throwsModerationException() {
        when(reviewAiService.moderate("spam text")).thenReturn("NOT_OK: spam detected");

        assertThatThrownBy(() -> service.moderate("spam text"))
                .isInstanceOf(ReviewModerationException.class)
                .hasMessageContaining("spam detected");
    }

    @Test
    @DisplayName("moderate: allows review through when AI is unavailable (fallback)")
    void moderate_aiUnavailable_doesNotThrow() {
        when(reviewAiService.moderate("some text")).thenThrow(new RuntimeException("timeout"));
        service.moderate("some text");
    }

    @Test
    @DisplayName("summarize: returns AI summary on success")
    void summarize_success_returnsSummary() {
        when(reviewAiService.summarize("Loved it", 5)).thenReturn("A highly positive review praising the coffee.");

        var result = service.summarize("Loved it", 5);

        assertThat(result).isEqualTo("A highly positive review praising the coffee.");
    }

    @Test
    @DisplayName("summarize: returns fallback when AI is unavailable")
    void summarize_aiUnavailable_returnsFallback() {
        when(reviewAiService.summarize("Loved it", 5)).thenThrow(new RuntimeException("timeout"));

        var result = service.summarize("Loved it", 5);

        assertThat(result).isEqualTo("Summary unavailable.");
    }
}
