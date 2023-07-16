package com.zufar.onlinestore.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@PropertySource("classpath:stripe.properties")
@RequiredArgsConstructor
@Configuration
public class StripeConfiguration {

    private final Environment env;

    @Bean
    public StripeTemplate stripeTemplate() {
        String pubKey = env.getProperty("stripe.publishable.key");
        String secKey = env.getProperty("stripe.secret.key");
        return new StripeTemplate(pubKey, secKey);
    }

}
