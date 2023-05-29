package com.zufar.onlinestore.notification.repository;

import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.notification.entity.Notification;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            repository.save(new Notification(1, "Notification1", customer));
            repository.save(new Notification(2, "Notification2", customer));
            repository.save(new Notification(3, "Notification3", customer));
            repository.save(new Notification(4, "Notification4", customer));
        };
    }
}
