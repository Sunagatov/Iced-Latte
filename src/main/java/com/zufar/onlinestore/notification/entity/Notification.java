package com.zufar.onlinestore.notification.entity;

import com.zufar.onlinestore.customer.entity.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

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
    private UUID id;

    @Column(name = "message")
    private String message;

    @OneToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "recipient_id", referencedColumnName = "id")
    private Customer recipient;
}