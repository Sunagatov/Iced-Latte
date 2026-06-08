package com.zufar.icedlatte.supportchat.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("TelegramWebhookController unit tests")
class TelegramWebhookControllerTest {

    private final TelegramWebhookService webhookService = mock(TelegramWebhookService.class);
    private final TelegramWebhookController controller = new TelegramWebhookController(webhookService);

    @Test
    @DisplayName("Unauthorized webhook result returns 401")
    void handle_unauthorized_returns401() {
        TelegramWebhookUpdate update = new TelegramWebhookUpdate(1L, null);
        when(webhookService.handle("wrong", update)).thenReturn(TelegramWebhookResult.UNAUTHORIZED);

        var response = controller.handle("wrong", update);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Processed webhook result returns 200")
    void handle_processed_returns200() {
        TelegramWebhookUpdate update = new TelegramWebhookUpdate(1L, null);
        when(webhookService.handle("secret", update)).thenReturn(TelegramWebhookResult.PROCESSED);

        var response = controller.handle("secret", update);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Ignored webhook result returns 200")
    void handle_ignored_returns200() {
        TelegramWebhookUpdate update = new TelegramWebhookUpdate(1L, null);
        when(webhookService.handle("secret", update)).thenReturn(TelegramWebhookResult.IGNORED);

        var response = controller.handle("secret", update);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
