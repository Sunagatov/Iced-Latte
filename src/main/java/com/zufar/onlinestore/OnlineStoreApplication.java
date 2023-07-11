package com.zufar.onlinestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
@EnableMongoAuditing
@SpringBootApplication
public class OnlineStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnlineStoreApplication.class, args);
    }
}