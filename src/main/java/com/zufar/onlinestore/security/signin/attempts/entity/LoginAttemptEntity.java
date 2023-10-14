package com.zufar.onlinestore.security.signin.attempts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Column(name = "user_email", nullable = false, unique = true)
    private String userEmail;

    @Column(name = "attempts", nullable = false)
    private AtomicInteger attempts;

    @Column(name = "expiration_datetime", nullable = true)
    private LocalDateTime expirationDatetime;

    @Column(name = "is_user_locked", nullable = false)
    private Boolean isUserLocked;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Version
    private Long version;

    public void incrementAttempts() {
        if (this.attempts == null) {
            this.attempts = new AtomicInteger(0);
        }
        this.attempts.incrementAndGet();
    }

    public void setAttempts(int initialLoginAttemptsCount) {
        this.attempts = new AtomicInteger(initialLoginAttemptsCount);
    }

    public int getAttempts() {
        return this.attempts.get();
    }
}
