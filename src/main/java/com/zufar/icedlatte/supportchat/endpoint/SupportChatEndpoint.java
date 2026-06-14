package com.zufar.icedlatte.supportchat.endpoint;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.openapi.dto.CreateSupportChatMessageRequest;
import com.zufar.icedlatte.openapi.dto.SupportChatConversationDto;
import com.zufar.icedlatte.openapi.dto.SupportChatMessageDto;
import com.zufar.icedlatte.openapi.dto.SupportChatMessagePageDto;
import com.zufar.icedlatte.openapi.dto.SupportChatStatusDto;
import com.zufar.icedlatte.openapi.supportchat.api.SupportChatApi;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.supportchat.converter.SupportChatDtoConverter;
import com.zufar.icedlatte.supportchat.service.SupportChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(ApiPaths.SUPPORT_CHAT)
public class SupportChatEndpoint implements SupportChatApi {

    private final CurrentUserProvider currentUserProvider;
    private final SupportChatService supportChatService;
    private final SupportChatDtoConverter converter;
    private final HttpServletRequest httpRequest;
    private final ClientIpExtractor clientIpExtractor;

    @Override
    @GetMapping("/status")
    public ResponseEntity<SupportChatStatusDto> getSupportChatStatus() {
        var status = supportChatService.status(currentUserProvider.get());
        return ResponseEntity.ok(converter.toStatusDto(status));
    }

    @Override
    @GetMapping("/conversation")
    public ResponseEntity<SupportChatConversationDto> getCurrentSupportChatConversation() {
        var conversation = supportChatService.getOrCreateConversation(currentUserProvider.get());
        return ResponseEntity.ok(converter.toConversationDto(conversation));
    }

    @Override
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<SupportChatMessagePageDto> getSupportChatMessages(
            @PathVariable UUID conversationId, Integer page, Integer size) {
        CurrentUserSnapshot user = currentUserProvider.get();
        size = size == null ? 20 : size;
        page = page == null ? 0 : page;
        var messages = supportChatService.getHistory(user, conversationId, page, size);
        return ResponseEntity.ok(converter.toMessagePageDto(messages));
    }

    @Override
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<SupportChatMessageDto> sendSupportChatMessage(
            @PathVariable UUID conversationId, @Valid @RequestBody CreateSupportChatMessageRequest request) {
        String turnstileToken = request.getTurnstileToken().orElse(null);
        UUID clientMessageId = request.getClientMessageId();
        CurrentUserSnapshot user = currentUserProvider.get();
        String clientIp = clientIpExtractor.extract(httpRequest);
        var message = supportChatService.sendCustomerMessage(
                user, conversationId, clientMessageId, request.getBody(), turnstileToken, clientIp);
        return ResponseEntity.ok(converter.toMessageDto(message));
    }
}
