package com.zufar.icedlatte.supportchat.exception;

public final class SupportChatDisabledException extends SupportChatException {

    public SupportChatDisabledException() {
        super("Support chat is unavailable.");
    }
}
