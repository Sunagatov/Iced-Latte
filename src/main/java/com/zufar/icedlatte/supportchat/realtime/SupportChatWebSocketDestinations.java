package com.zufar.icedlatte.supportchat.realtime;

import java.util.UUID;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SupportChatWebSocketDestinations {

    public static final String CONVERSATION_MESSAGES_PREFIX = "/topic/support-chat/conversations/";
    public static final String CONVERSATION_MESSAGES_SUFFIX = "/messages";

    public static String conversationMessages(UUID conversationId) {
        return CONVERSATION_MESSAGES_PREFIX + conversationId + CONVERSATION_MESSAGES_SUFFIX;
    }
}
