package com.zufar.icedlatte.supportchat.exception;

public final class SupportChatConversationNotFoundException extends SupportChatException {

    public SupportChatConversationNotFoundException() {
        super("Support conversation not found.");
    }
}
