package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;

@DisplayName("AvatarUploadActivationService Spring context tests")
class AvatarUploadActivationServiceContextTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(AvatarUploadActivationServiceConfiguration.class);

    @Test
    @DisplayName("creates AvatarUploadActivationService in Spring context")
    void createsAvatarUploadActivationServiceInSpringContext() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AvatarUploadActivationService.class);
        });
    }

    @Configuration
    @Import(AvatarUploadActivationService.class)
    static class AvatarUploadActivationServiceConfiguration {

        @Bean
        UserAvatarUploadRepository userAvatarUploadRepository() {
            return mock(UserAvatarUploadRepository.class);
        }

        @Bean
        FileStorageWriterApi fileStorageWriterApi() {
            return mock(FileStorageWriterApi.class);
        }
    }
}
