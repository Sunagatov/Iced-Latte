package com.zufar.icedlatte.supportchat.owner;

public interface OwnerMessageSender {

    OwnerMessageDeliveryResult send(OwnerMessage message);
}
