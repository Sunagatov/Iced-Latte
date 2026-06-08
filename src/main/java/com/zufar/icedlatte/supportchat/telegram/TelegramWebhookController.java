package com.zufar.icedlatte.supportchat.telegram;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.http.ApiPaths;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.SUPPORT_CHAT_TELEGRAM_WEBHOOK)
class TelegramWebhookController {

    static final String SECRET_TOKEN_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramWebhookService webhookService;

    @PostMapping
    ResponseEntity<Void> handle(
            @RequestHeader(name = SECRET_TOKEN_HEADER, required = false) String secretToken,
            @RequestBody(required = false) TelegramWebhookUpdate update) {
        TelegramWebhookResult result = webhookService.handle(secretToken, update);
        if (result == TelegramWebhookResult.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }
}
