package com.zufar.icedlatte.order.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order")
@Getter
@Setter
public class OrderConfig {

    private int cancellationWindowMinutes = 30;
}
