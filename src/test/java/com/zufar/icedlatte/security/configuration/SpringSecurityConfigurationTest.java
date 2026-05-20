package com.zufar.icedlatte.security.configuration;

import com.zufar.icedlatte.common.correlation.CorrelationFilter;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;
import com.zufar.icedlatte.security.jwt.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SpringSecurityConfiguration")
class SpringSecurityConfigurationTest {

    private final SpringSecurityConfiguration configuration =
            new SpringSecurityConfiguration(new ProblemTypeUriFactory("https://errors.example.test/problems"));

    @Test
    @DisplayName("disables duplicate servlet registration for correlation filter")
    void disablesDuplicateServletRegistrationForCorrelationFilter() {
        CorrelationFilter filter = mock(CorrelationFilter.class);

        FilterRegistrationBean<CorrelationFilter> registration =
                configuration.correlationFilterRegistration(filter);

        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getFilter()).isSameAs(filter);
    }

    @Test
    @DisplayName("disables duplicate servlet registration for JWT filter")
    void disablesDuplicateServletRegistrationForJwtFilter() {
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                configuration.jwtFilterRegistration(filter);

        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getFilter()).isSameAs(filter);
    }

    @Test
    @DisplayName("builds DAO authentication provider with explicit user-not-found behavior")
    void buildsDaoAuthenticationProviderWithExplicitUserNotFoundBehavior() {
        UserDetailsService userDetailsService =
                username -> User.withUsername(username).password("encoded").authorities("ROLE_USER").build();
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        AuthenticationProvider provider =
                configuration.authenticationProvider(userDetailsService, passwordEncoder);

        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
        DaoAuthenticationProvider daoProvider = (DaoAuthenticationProvider) provider;
        assertThat(daoProvider.isHideUserNotFoundExceptions()).isFalse();
    }

    @Test
    @DisplayName("returns authentication manager from authentication configuration")
    void returnsAuthenticationManagerFromAuthenticationConfiguration() {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertThat(configuration.authenticationManager(authenticationConfiguration)).isSameAs(authenticationManager);
    }

    @Test
    @DisplayName("wraps authentication manager build failures")
    void wrapsAuthenticationManagerBuildFailures() {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        RuntimeException cause = new RuntimeException("boom");
        when(authenticationConfiguration.getAuthenticationManager()).thenThrow(cause);

        assertThatThrownBy(() -> configuration.authenticationManager(authenticationConfiguration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to build AuthenticationManager")
                .hasCause(cause);
    }

    @Test
    @DisplayName("builds argon2 password encoder with valid parameters")
    void buildsArgon2PasswordEncoderWithValidParameters() {
        PasswordEncoder encoder = configuration.passwordEncoder(16384, 2);

        assertThat(encoder).isInstanceOf(Argon2PasswordEncoder.class);
    }

    @Test
    @DisplayName("rejects too-small argon2 memory")
    void rejectsTooSmallArgon2Memory() {
        assertThatThrownBy(() -> configuration.passwordEncoder(512, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("security.argon2.memory must be at least 1024 KB, got: 512");
    }

    @Test
    @DisplayName("rejects non-positive argon2 iterations")
    void rejectsNonPositiveArgon2Iterations() {
        assertThatThrownBy(() -> configuration.passwordEncoder(16384, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("security.argon2.iterations must be at least 1, got: 0");
    }
}
