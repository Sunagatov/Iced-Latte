package com.zufar.onlinestore.payment.api.impl.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.payment.converter.StripeCustomerConverter;
import com.zufar.onlinestore.payment.exception.StripeCustomerProcessingException;
import com.zufar.onlinestore.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class StripeCustomerCreator {

    private final StripeCustomerConverter stripeCustomerConverter;

    public Customer createStripeCustomer(UserEntity authorizedUser, String paymentMethodToken) {
        try {
            log.info("Create stripe customer: in progress: start stripe customer creation");
            CustomerCreateParams customerCreateParams = stripeCustomerConverter.toStripeObject(authorizedUser, paymentMethodToken);
            Customer createdStripeCustomer = Customer.create(customerCreateParams);
            String createdStripeCustomerId = createdStripeCustomer.getId();
            log.info("Create stripe customer: successful: stripe customer was created with createdStripeCustomerId = {}.", createdStripeCustomerId);

            return createdStripeCustomer;

        } catch (StripeException e) {
            log.error("Process stripe customer: failed: stripe customer was not created");
            throw new StripeCustomerProcessingException(authorizedUser.getEmail());
        }
    }
}
