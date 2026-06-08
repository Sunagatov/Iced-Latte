package com.zufar.icedlatte.supportchat.owner;

import java.util.UUID;

public record OwnerMessage(UUID conversationId, UUID messageId, UUID userId, String customerEmail, String body) {}
