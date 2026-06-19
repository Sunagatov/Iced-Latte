package com.zufar.icedlatte.supportchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageSender;
import com.zufar.icedlatte.supportchat.realtime.SupportChatMessagePublisher;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;

@DisplayName("SupportChatMessageFlowService Spring context tests")
class SupportChatMessageFlowServiceContextTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(SupportChatMessageFlowServiceConfiguration.class);

    @Test
    @DisplayName("creates SupportChatMessageFlowService when both rate limiter beans exist")
    void createsSupportChatMessageFlowServiceWhenBothRateLimiterBeansExist() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SupportChatMessageFlowService.class);
            assertThat(context).hasSingleBean(SupportChatMessageBodyPolicy.class);
            assertThat(context).hasSingleBean(SupportChatCustomerMessagePolicy.class);
        });
    }

    @Configuration
    @Import({
        SupportChatMessageFlowService.class,
        SupportChatMessageBodyPolicy.class,
        SupportChatCustomerMessagePolicy.class
    })
    static class SupportChatMessageFlowServiceConfiguration {

        @Bean
        SupportChatProperties supportChatProperties() {
            return mock(SupportChatProperties.class);
        }

        @Bean
        SupportChatAvailabilityService supportChatAvailabilityService() {
            return mock(SupportChatAvailabilityService.class);
        }

        @Bean
        SupportConversationRepository supportConversationRepository() {
            return mock(SupportConversationRepository.class);
        }

        @Bean
        SupportMessageRepository supportMessageRepository() {
            return mock(SupportMessageRepository.class);
        }

        @Bean
        OwnerMessageSender ownerMessageSender() {
            return mock(OwnerMessageSender.class);
        }

        @Bean
        SupportChatMessagePublisher supportChatMessagePublisher() {
            return mock(SupportChatMessagePublisher.class);
        }

        @Bean
        TurnstileVerifier turnstileVerifier() {
            return mock(TurnstileVerifier.class);
        }

        @Bean
        SupportChatAbuseGuard supportChatAbuseGuard() {
            return mock(SupportChatAbuseGuard.class);
        }

        @Bean("openRateLimiter")
        RateLimiter openRateLimiter() {
            return mock(RateLimiter.class);
        }

        @Bean("closedRateLimiter")
        RateLimiter closedRateLimiter() {
            return mock(RateLimiter.class);
        }

        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return mock(PlatformTransactionManager.class);
        }
    }
}
