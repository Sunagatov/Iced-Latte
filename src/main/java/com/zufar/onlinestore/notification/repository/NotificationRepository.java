package com.zufar.onlinestore.notification.repository;


import com.zufar.onlinestore.notification.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, Integer> {
}

