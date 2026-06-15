package com.zufar.icedlatte.supportchat.telegram;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "support-chat.owner-message-mode", havingValue = "TELEGRAM")
class TelegramBotRestClient implements TelegramBotClient {

    private static final String TELEGRAM_BOT_API_BASE_URL = "https://api.telegram.org/bot";

    private final String chatId;
    private final RestClient restClient;

    TelegramBotRestClient(SupportChatProperties properties) {
        SupportChatProperties.Telegram telegram = properties.telegram();
        this.chatId = telegram.chatId();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(telegram.connectTimeout());
        requestFactory.setReadTimeout(telegram.readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(TELEGRAM_BOT_API_BASE_URL + telegram.botToken())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<TelegramForumTopic> createForumTopic(String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("name", name);

        try {
            TelegramCreateForumTopicResponse response = restClient
                    .post()
                    .uri("/createForumTopic")
                    .body(body)
                    .retrieve()
                    .body(TelegramCreateForumTopicResponse.class);
            if (isInvalidResponse(response)) {
                log.warn("support_chat.telegram.create_forum_topic.failed");
                return Optional.empty();
            }

            long messageThreadId = response.result().messageThreadId();
            TelegramForumTopic telegramForumTopic = new TelegramForumTopic(messageThreadId);

            return Optional.of(telegramForumTopic);

        } catch (RuntimeException ex) {
            String logMessage = "support_chat.telegram.create_forum_topic.error: exceptionClass={}";
            log.warn(logMessage, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<TelegramMessageRef> sendMessage(Long messageThreadId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("link_preview_options", Map.of("is_disabled", true));
        if (messageThreadId != null) {
            body.put("message_thread_id", messageThreadId);
        }

        try {
            TelegramSendMessageResponse response =
                    restClient.post().uri("/sendMessage").body(body).retrieve().body(TelegramSendMessageResponse.class);
            if (isInvalidResponse(response)) {
                log.warn("support_chat.telegram.send_message.failed");
                return Optional.empty();
            }
            long messageId = response.result().messageId();
            TelegramMessageRef telegramMessageRef = new TelegramMessageRef(messageId);
            return Optional.of(telegramMessageRef);
        } catch (RuntimeException ex) {
            String logMessage = "support_chat.telegram.send_message.error: exceptionClass={}";
            log.warn(logMessage, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static boolean isInvalidResponse(TelegramApiResponse response) {
        return response == null || !response.ok() || response.result() == null;
    }

    private interface TelegramApiResponse {
        boolean ok();

        Object result();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramCreateForumTopicResponse(boolean ok, TelegramCreateForumTopicResult result)
            implements TelegramApiResponse {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramCreateForumTopicResult(
            @JsonProperty("message_thread_id") long messageThreadId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramSendMessageResponse(boolean ok, TelegramSendMessageResult result)
            implements TelegramApiResponse {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramSendMessageResult(
            @JsonProperty("message_id") long messageId) {}
}
