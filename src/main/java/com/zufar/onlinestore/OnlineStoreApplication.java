package com.zufar.onlinestore;

import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.reservation.config.ReservationTimeoutConfiguration;
import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@AllArgsConstructor
@EnableConfigurationProperties(value = {StripeConfiguration.class, ReservationTimeoutConfiguration.class})
public class OnlineStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineStoreApplication.class, args);
    }
}