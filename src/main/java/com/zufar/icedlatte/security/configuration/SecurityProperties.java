package com.zufar.icedlatte.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Security configuration properties for enhanced security features.
 * Uses Java 21 record features for immutable configuration.
 */
@Validated
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        
        @NotNull(message = "Rate limiting configuration cannot be null")
        RateLimit rateLimit,
        
        @NotNull(message = "Session configuration cannot be null")
        Session session,
        
        @NotNull(message = "Password policy cannot be null")
        PasswordPolicy passwordPolicy
) {
    
    public record RateLimit(
            @Min(value = 1, message = "Max requests must be at least 1")
            int maxRequests,
            
            @NotNull(message = "Window duration cannot be null")
            Duration windowDuration,
            
            @Min(value = 1, message = "Auth max requests must be at least 1")
            int authMaxRequests,
            
            @NotNull(message = "Auth window duration cannot be null")
            Duration authWindowDuration,
            
            @Min(value = 1, message = "Payment max requests must be at least 1")
            int paymentMaxRequests,
            
            @NotNull(message = "Payment window duration cannot be null")
            Duration paymentWindowDuration
    ) {
        public RateLimit {
            if (maxRequests <= 0) maxRequests = 100;
            if (windowDuration == null) windowDuration = Duration.ofMinutes(1);
            if (authMaxRequests <= 0) authMaxRequests = 5;
            if (authWindowDuration == null) authWindowDuration = Duration.ofMinutes(1);
            if (paymentMaxRequests <= 0) paymentMaxRequests = 10;
            if (paymentWindowDuration == null) paymentWindowDuration = Duration.ofMinutes(1);
        }
    }
    
    public record Session(
            @Min(value = 1, message = "Maximum sessions must be at least 1")
            int maximumSessions,
            
            boolean preventLogin,
            
            @NotNull(message = "Session timeout cannot be null")
            Duration timeout
    ) {
        public Session {
            if (maximumSessions <= 0) maximumSessions = 1;
            if (timeout == null) timeout = Duration.ofMinutes(30);
        }
    }
    
    public record PasswordPolicy(
            @Min(value = 8, message = "Minimum password length must be at least 8")
            int minLength,
            
            @Min(value = 1, message = "Minimum uppercase characters must be at least 1")
            int minUppercase,
            
            @Min(value = 1, message = "Minimum lowercase characters must be at least 1")
            int minLowercase,
            
            @Min(value = 1, message = "Minimum digits must be at least 1")
            int minDigits,
            
            @Min(value = 1, message = "Minimum special characters must be at least 1")
            int minSpecialChars,
            
            @NotNull(message = "Password history cannot be null")
            Duration historyRetention
    ) {
        public PasswordPolicy {
            if (minLength < 8) minLength = 8;
            if (minUppercase < 1) minUppercase = 1;
            if (minLowercase < 1) minLowercase = 1;
            if (minDigits < 1) minDigits = 1;
            if (minSpecialChars < 1) minSpecialChars = 1;
            if (historyRetention == null) historyRetention = Duration.ofDays(90);
        }
    }
    
    public SecurityProperties {
        if (rateLimit == null) {
            rateLimit = new RateLimit(100, Duration.ofMinutes(1), 5, Duration.ofMinutes(1), 10, Duration.ofMinutes(1));
        }
        if (session == null) {
            session = new Session(1, false, Duration.ofMinutes(30));
        }
        if (passwordPolicy == null) {
            passwordPolicy = new PasswordPolicy(8, 1, 1, 1, 1, Duration.ofDays(90));
        }
    }
}
