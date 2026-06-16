package com.zufar.icedlatte.security.oauth.login;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import com.zufar.icedlatte.security.oauth.config.GitHubOAuthProperties;

@DisplayName("GitHubTokenExchanger Spring context tests")
class GitHubTokenExchangerContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GitHubTokenExchangerConfiguration.class)
            .withPropertyValues(
                    "github.enabled=true",
                    "github.client-id=test-client",
                    "github.client-secret=test-secret",
                    "github.redirect-uri=https://app.example.com/callback",
                    "github.scope=read:user user:email",
                    "github.api-version=2026-03-10",
                    "github.timeout.connect-timeout=PT2S",
                    "github.timeout.read-timeout=PT3S");

    @Test
    @DisplayName("creates the GitHub OAuth provider bean when GitHub auth is enabled")
    void createsTheGitHubOauthProviderBeanWhenGitHubAuthIsEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GitHubTokenExchanger.class);
        });
    }

    @Test
    @DisplayName("does not create the GitHub OAuth provider bean when GitHub auth is disabled")
    void doesNotCreateTheGitHubOauthProviderBeanWhenGitHubAuthIsDisabled() {
        contextRunner.withPropertyValues("github.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(GitHubTokenExchanger.class);
        });
    }

    @EnableConfigurationProperties(GitHubOAuthProperties.class)
    @Import(GitHubTokenExchanger.class)
    static class GitHubTokenExchangerConfiguration {}
}
