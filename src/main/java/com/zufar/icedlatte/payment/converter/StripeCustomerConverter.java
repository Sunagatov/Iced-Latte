package com.zufar.icedlatte.payment.converter;

import com.stripe.param.CustomerCreateParams;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class StripeCustomerConverter {

    public CustomerCreateParams toStripeObject(UserEntity authorizedUser, String paymentMethodToken) {
        if ( authorizedUser == null && paymentMethodToken == null ) {
            return null;
        }

        CustomerCreateParams.Builder customerCreateParams = CustomerCreateParams.builder();

        if ( authorizedUser != null ) {
            customerCreateParams.setEmail( authorizedUser.getEmail() );
        }
        customerCreateParams.setPaymentMethod( paymentMethodToken );
        customerCreateParams.setAddress( toAddress(authorizedUser.getAddress()) );
        customerCreateParams.setName( toFullName(authorizedUser) );

        return customerCreateParams.build();
    }

    private CustomerCreateParams.Address toAddress(Address address) {
        CustomerCreateParams.Address stripeAddress = null;
        if (address != null) {
            stripeAddress = CustomerCreateParams.Address.builder()
                    .setCountry(address.getCountry())
                    .setCity(address.getCity())
                    .setLine1(address.getLine())
                    .build();
        }
        return stripeAddress;
    }

    private String toFullName(UserEntity authorizedUser) {
        return StringUtils.join(authorizedUser.getFirstName(), Character.SPACE_SEPARATOR, authorizedUser.getLastName());
    }
}

