package com.zufar.onlinestore.payment.api.impl.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.payment.converter.StripeCustomerConverter;
import com.zufar.onlinestore.payment.exception.StripeCustomerCreationException;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * This class is responsible for Stipe customer creation. It needed in order to connect
 * customer with payment intent, this will ensure the correct management of payment by Stripe API
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class StripeCustomerCreator {

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final StripeCustomerConverter stripeCustomerConverter;

    public Customer createStripeCustomer() throws StripeCustomerCreationException {
        UserDto authorizedUser = securityPrincipalProvider.get();
        CustomerCreateParams customerCreateParams = stripeCustomerConverter.toStripeObject(authorizedUser);
        String customerEmail = customerCreateParams.getEmail();
        log.info("Create stripe customer: in progress: creation stripe customer with email: {}.", customerEmail);
        try {
            Customer customer = Customer.create(customerCreateParams);
            log.info("Create stripe customer: successful: stripe customer was created with id: {}.", customer.getId());
            return customer;
        } catch (StripeException e) {
            log.error("Create stripe customer: failed: stripe customer was not created");
            throw new StripeCustomerCreationException(customerEmail);
        }
    }
}
