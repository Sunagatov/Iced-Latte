package com.zufar.onlinestore.payment.api.impl.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.payment.converter.StripeCustomerConverter;
import com.zufar.onlinestore.payment.exception.StripeCustomerProcessingException;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
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
public class StripeCustomerProcessor {

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final StripeCustomerConverter stripeCustomerConverter;
    private final UserRepository userRepository;

    public Customer processStripeCustomer() throws StripeCustomerProcessingException {
        UUID authorizedUserId = securityPrincipalProvider.getUserId();
        UserEntity authorizedUser = userRepository.findById(authorizedUserId).orElseGet(UserEntity::new);
        Map<String, String> metadata = Map.of("authorizedUserId", authorizedUserId.toString());

        String authorizedUserEmail = authorizedUser.getEmail();
        log.info("Process stripe customer: in progress: processing stripe customer with authorizedUserEmail = {}.", authorizedUserEmail);
        try {
            if (authorizedUser.getStripeCustomerId() != null) {
                Customer retrievedStripeCustomer = Customer.retrieve(authorizedUser.getStripeCustomerId());
                log.info("Process stripe customer: successfully: retrieved stripe customer with retrievedStripeCustomerId = {}.", retrievedStripeCustomer.getId());

                return retrievedStripeCustomer;
            }

            CustomerCreateParams customerCreateParams = stripeCustomerConverter.toStripeObject(authorizedUser, metadata);
            Customer customer = Customer.create(customerCreateParams);
            String stripeCustomerId = customer.getId();
            log.info("Create stripe customer: successful: stripe customer was created with customerId = {}.", stripeCustomerId);
            authorizedUser.setStripeCustomerId(stripeCustomerId);

            userRepository.save(authorizedUser);

            return customer;
        } catch (StripeException e) {
            log.error("Process stripe customer: failed: stripe customer was not processed");
            throw new StripeCustomerProcessingException(authorizedUserEmail);
        }
    }
}
