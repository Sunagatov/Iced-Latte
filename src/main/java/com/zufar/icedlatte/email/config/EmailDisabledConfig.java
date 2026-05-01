package com.zufar.icedlatte.email.config;

import com.zufar.icedlatte.email.sender.AuthTokenEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "email.enabled", havingValue = "false", matchIfMissing = true)
public class EmailDisabledConfig {

    @Bean
    public AuthTokenEmailSender noOpAuthTokenEmailSender() {
        return (_, _) -> log.debug("email.send.skipped: email.enabled=false");
    }
}
