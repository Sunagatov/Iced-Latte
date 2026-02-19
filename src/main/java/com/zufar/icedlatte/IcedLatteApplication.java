package com.zufar.icedlatte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableRetry
@EnableJpaRepositories(basePackages = {
    "com.zufar.icedlatte.cart.repository",
    "com.zufar.icedlatte.common.audit",
    "com.zufar.icedlatte.favorite.repository",
    "com.zufar.icedlatte.filestorage.repository",
    "com.zufar.icedlatte.order.repository",
    "com.zufar.icedlatte.product.repository",
    "com.zufar.icedlatte.review.repository",
    "com.zufar.icedlatte.security.repository",
    "com.zufar.icedlatte.user.repository"
})
@EnableRedisRepositories(basePackages = "com.zufar.icedlatte.redis")
public class IcedLatteApplication {

    public static void main(String[] args) {
        SpringApplication.run(IcedLatteApplication.class, args);
    }
}
