package com.zufar.icedlatte.supportchat.exception;

public abstract sealed class SupportChatException extends RuntimeException
        permits DuplicateSupportChatMessageException,
                InvalidSupportChatMessageException,
                SupportChatConversationNotFoundException,
                SupportChatDisabledException,
                SupportChatEmailVerificationRequiredException,
                SupportChatRateLimitExceededException {

    protected SupportChatException(String message) {
        super(message);
    }
}
