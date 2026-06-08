package com.zufar.icedlatte.supportchat.owner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "support-chat.owner-message-mode", havingValue = "FAKE")
public class FakeOwnerMessageSender implements OwnerMessageSender {

    @Override
    public OwnerMessageDeliveryResult send(OwnerMessage message) {
        return OwnerMessageDeliveryResult.deliveredResult();
    }
}
