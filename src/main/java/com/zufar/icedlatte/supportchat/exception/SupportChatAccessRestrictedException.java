package com.zufar.icedlatte.supportchat.exception;

public final class SupportChatAccessRestrictedException extends SupportChatException {

    public SupportChatAccessRestrictedException() {
        super("Support chat is not available for this account.");
    }
}
