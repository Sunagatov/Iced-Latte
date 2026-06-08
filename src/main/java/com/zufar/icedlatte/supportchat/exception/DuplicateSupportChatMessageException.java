package com.zufar.icedlatte.supportchat.exception;

public final class DuplicateSupportChatMessageException extends SupportChatException {

    public DuplicateSupportChatMessageException() {
        super("Repeated message rejected.");
    }
}
