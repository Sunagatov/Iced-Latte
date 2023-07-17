package com.zufar.onlinestore.notification.repository;

import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.notification.entity.Notification;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * @author Alex Zarubin
 * created on 27.05.2023
 */
@Configuration
public class NotificationsMongoDatabaseInitializer {


    @Bean
    CommandLineRunner notificationCommandLineRunner(NotificationRepository repository) {
        //TODO: remove this
        Customer customer = new Customer();

        return strings -> {
            repository.save(new Notification(UUID.randomUUID(), "Notification1", customer));
            repository.save(new Notification(UUID.randomUUID(), "Notification2", customer));
            repository.save(new Notification(UUID.randomUUID(), "Notification3", customer));
            repository.save(new Notification(UUID.randomUUID(), "Notification4", customer));
        };
    }
}