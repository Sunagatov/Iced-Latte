package com.zufar.icedlatte.supportchat.owner;

public record OwnerMessageDeliveryResult(boolean delivered) {

    public static OwnerMessageDeliveryResult deliveredResult() {
        return new OwnerMessageDeliveryResult(true);
    }

    public static OwnerMessageDeliveryResult failedResult() {
        return new OwnerMessageDeliveryResult(false);
    }
}
