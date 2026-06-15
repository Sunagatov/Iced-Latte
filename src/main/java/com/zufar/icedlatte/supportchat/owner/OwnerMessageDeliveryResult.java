package com.zufar.icedlatte.supportchat.owner;

public record OwnerMessageDeliveryResult(boolean delivered, Long telegramMessageId) {

    public static OwnerMessageDeliveryResult deliveredResult() {
        return new OwnerMessageDeliveryResult(true, null);
    }

    public static OwnerMessageDeliveryResult deliveredResult(long telegramMessageId) {
        return new OwnerMessageDeliveryResult(true, telegramMessageId);
    }

    public static OwnerMessageDeliveryResult failedResult() {
        return new OwnerMessageDeliveryResult(false, null);
    }
}
