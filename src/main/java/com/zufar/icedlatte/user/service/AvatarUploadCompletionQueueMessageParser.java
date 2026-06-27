package com.zufar.icedlatte.user.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AvatarUploadCompletionQueueMessageParser {

    private final ObjectMapper objectMapper;

    public AvatarUploadCompletionQueueMessage parse(String body) {
        try {
            AvatarUploadCompletionPayload payload = objectMapper.readValue(body, AvatarUploadCompletionPayload.class);
            validateEnvelope(payload);
            return payload.toQueueMessage();
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Avatar upload completion message JSON is invalid.");
        }
    }

    private void validateEnvelope(AvatarUploadCompletionPayload payload) {
        if (!AvatarUploadCompletionPayload.EVENT_TYPE.equals(payload.eventType())) {
            throw new BadRequestException("Avatar upload completion message eventType is unsupported.");
        }
        if (payload.version() == null || payload.version() != AvatarUploadCompletionPayload.VERSION) {
            throw new BadRequestException("Avatar upload completion message version is unsupported.");
        }
    }
}
