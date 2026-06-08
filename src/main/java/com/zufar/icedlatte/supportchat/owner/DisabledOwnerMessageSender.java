package com.zufar.icedlatte.supportchat.owner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(OwnerMessageSender.class)
public class DisabledOwnerMessageSender implements OwnerMessageSender {

    @Override
    public OwnerMessageDeliveryResult send(OwnerMessage message) {
        return OwnerMessageDeliveryResult.failedResult();
    }
}
