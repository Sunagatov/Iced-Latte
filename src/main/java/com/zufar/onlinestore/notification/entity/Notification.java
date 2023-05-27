package com.zufar.onlinestore.notification.entity;

import com.zufar.onlinestore.customer.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Alex Zarubin
 * created on 24.05.2023
 */


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
public class Notification {

    @Id
    private int id;

    private String message;
    private Customer recipient;
}

