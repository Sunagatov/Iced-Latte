package com.zufar.icedlatte.common.config;

import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class PyroscopeConfig {

    @Value("${pyroscope.server-address:}")
    private String serverAddress;

    @Value("${pyroscope.application-name:iced-latte-backend}")
    private String applicationName;

    @Value("${pyroscope.basic-auth-user:}")
    private String basicAuthUser;

    @Value("${pyroscope.basic-auth-password:}")
    private String basicAuthPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void startPyroscope() {
        if (serverAddress == null || serverAddress.isBlank()) {
            return;
        }
        PyroscopeAgent.start(
            new PyroscopeAgent.Options.Builder(
                new Config.Builder()
                    .setApplicationName(applicationName)
                    .setServerAddress(serverAddress)
                    .setBasicAuthUser(basicAuthUser)
                    .setBasicAuthPassword(basicAuthPassword)
                    .build()
            ).build()
        );
    }
}
