package com.zufar.icedlatte.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "login_attempts")
public class LoginAttemptEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
// amazonq-ignore-next-line

    @Column(name = "user_email", nullable = false, unique = true, length = 254)
    private String userEmail;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "expiration_datetime")
    private Instant expirationDatetime;

    @Column(name = "is_user_locked", nullable = false)
    private Boolean isUserLocked;

    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;
}
