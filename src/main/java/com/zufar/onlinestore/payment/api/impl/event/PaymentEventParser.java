package com.zufar.onlinestore.payment.api.impl.event;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for parsing payment event to payment intent object.
 * */

@Slf4j
@Service
public class PaymentEventParser {

    public PaymentIntent parseEventToPaymentIntent(final Event event) {
        log.info("Parse event to payment intent: start payment event deserialization: event: {}", event);
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        return dataObjectDeserializer.getObject()
                .filter(PaymentIntent.class::isInstance)
                .map(PaymentIntent.class::cast)
                .orElse(new PaymentIntent());
    }
}
