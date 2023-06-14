package com.zufar.onlinestore.notification.entity;

import com.zufar.onlinestore.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Alex Zarubin
 * created on 24.05.2023
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(name = "message", nullable = false)
    private String message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    public Customer recipient;

}

