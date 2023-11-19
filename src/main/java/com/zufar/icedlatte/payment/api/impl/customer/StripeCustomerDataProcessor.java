package com.zufar.icedlatte.payment.api.impl.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.zufar.icedlatte.payment.exception.CustomerRetrievingException;
import com.zufar.icedlatte.payment.exception.PaymentMethodNotFoundException;
import com.zufar.icedlatte.payment.exception.PaymentMethodRetrievingException;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * This class responsible for Stipe customer creation. It needed in order to connect
 * customer with payment intent, this will ensure the correct management of payment by Stripe API
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StripeCustomerDataProcessor {

    private final StripePaymentMethodProcessor stripePaymentMethodProcessor;
    private final StripeCustomerCreator stripeCustomerCreator;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final UserRepository userRepository;

    public Pair<UUID, PaymentMethod> processStripeCustomerData(final String cardDetailsTokenId) {
        log.info("Process stripe customer data: starting: start customer data processing");
        UUID authorizedUserId = securityPrincipalProvider.getUserId();
        UserEntity authorizedUser = userRepository.findById(authorizedUserId)
                .orElseThrow(() -> new UserNotFoundException(authorizedUserId));

        String stripeCustomerToken = authorizedUser.getStripeCustomerToken();

        if (stripeCustomerToken != null) {
            Customer retrievedStripeCustomer = retrieveStripeCustomer(stripeCustomerToken);
            PaymentMethod retrievedStripePaymentMethod = retrieveStripePaymentMethodByStripeCustomer(retrievedStripeCustomer);
            log.info("Process stripe customer data: finished: customer data was processed");

            return Pair.of(authorizedUserId, retrievedStripePaymentMethod);

        } else {
            PaymentMethod createdStripePaymentMethod = stripePaymentMethodProcessor.processStripePaymentMethod(cardDetailsTokenId);
            Customer createdStripeCustomer = stripeCustomerCreator.createStripeCustomer(authorizedUser, createdStripePaymentMethod.getId());
            createdStripePaymentMethod.setCustomer(createdStripeCustomer.getId());

            authorizedUser.setStripeCustomerToken(createdStripeCustomer.getId());
            userRepository.save(authorizedUser);
            log.info("Process stripe customer data: finished: customer data was processed");

            return Pair.of(authorizedUserId, createdStripePaymentMethod);
        }
    }

    private PaymentMethod retrieveStripePaymentMethodByStripeCustomer(Customer retrievedStripeCustomer) {
        try {
            log.info("Retrieve stripe payment method by stripe customer: in progress: start stripe payment retrieving.");

            PaymentMethod paymentMethod = retrievedStripeCustomer.listPaymentMethods()
                    .getData().stream()
                    .findFirst()
                    .orElseThrow(() -> new PaymentMethodNotFoundException(retrievedStripeCustomer.getId()));

            log.info("Retrieve stripe payment method by stripe customer: successfully: stripe payment method was retrieved.");

            return paymentMethod;

        } catch (StripeException e) {
            log.error("Retrieve stripe payment method by stripe customer: failed: stripe payment method was not retrieved");
            throw new PaymentMethodRetrievingException(retrievedStripeCustomer.getId());
        }
    }

    private Customer retrieveStripeCustomer(String stripeCustomerToken) {
        try {
            log.info("Retrieve stripe customer: in progress: start stripe customer retrieving by stripeCustomerToken = {}.", stripeCustomerToken);
            Customer retrievedStripeCustomer = Customer.retrieve(stripeCustomerToken);
            log.info("Retrieve stripe customer: successfully: stripe customer by stripeCustomerToken = {} was retrieved.", stripeCustomerToken);

            return retrievedStripeCustomer;

        } catch (StripeException e) {
            log.error("Retrieve stripe customer: failed: stripe customer was not retrieved");
            throw new CustomerRetrievingException(stripeCustomerToken);
        }
    }
}