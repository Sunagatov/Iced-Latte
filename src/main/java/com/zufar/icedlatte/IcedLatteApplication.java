package com.zufar.icedlatte;

import com.zufar.icedlatte.payment.config.StripeConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(StripeConfiguration.class)
@EnableScheduling
public class IcedLatteApplication {

    public static void main(String[] args) {
        SpringApplication.run(IcedLatteApplication.class, args);
    }
}
