package com.zufar.onlinestore;

import com.zufar.onlinestore.payment.config.StripeConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(StripeConfiguration.class)
@EnableScheduling
public class OnlineStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineStoreApplication.class, args);
    }
}
