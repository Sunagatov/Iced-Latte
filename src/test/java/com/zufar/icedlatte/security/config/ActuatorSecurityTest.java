package com.zufar.icedlatte.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.correlation.CorrelationFilter;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;
import com.zufar.icedlatte.security.config.ActuatorSecurityTest.TestBeans;
import com.zufar.icedlatte.security.jwt.filter.JwtAuthenticationFilter;

@SpringJUnitWebConfig(
        classes = {
            SpringSecurityConfiguration.class,
            SecurityRouteAuthorization.class,
            SecurityProblemResponseWriter.class,
            TestBeans.class,
            ActuatorSecurityTest.ActuatorController.class
        })
@DisplayName("Actuator security")
class ActuatorSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "rateLimitingFilter")
    @Qualifier("rateLimitingFilter")
    private Filter rateLimitingFilter;

    @MockitoBean
    private CorrelationFilter correlationFilter;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        org.mockito.Mockito.doAnswer(invocation -> {
                    applyBearerAuthentication(invocation.getArgument(0));
                    invocation
                            .<FilterChain>getArgument(2)
                            .doFilter(invocation.getArgument(0), invocation.getArgument(1));
                    return null;
                })
                .when(jwtAuthenticationFilter)
                .doFilter(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doAnswer(ActuatorSecurityTest::continueFilterChain)
                .when(rateLimitingFilter)
                .doFilter(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doAnswer(ActuatorSecurityTest::continueFilterChain)
                .when(correlationFilter)
                .doFilter(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @DisplayName("prometheus metrics require authentication")
    void prometheusMetricsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("prometheus metrics reject non-admin users")
    void prometheusMetricsRejectNonAdminUsers() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("prometheus metrics allow admin users")
    void prometheusMetricsAllowAdminUsers() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("health remains public")
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private static Object continueFilterChain(org.mockito.invocation.InvocationOnMock invocation) throws Exception {
        invocation.<FilterChain>getArgument(2).doFilter(invocation.getArgument(0), invocation.getArgument(1));
        return null;
    }

    private static void applyBearerAuthentication(ServletRequest request) {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            return;
        }
        String authorization = httpRequest.getHeader("Authorization");
        if ("Bearer admin-token".equals(authorization)) {
            authenticate("ROLE_ADMIN");
        } else if ("Bearer user-token".equals(authorization)) {
            authenticate("USER");
        } else {
            SecurityContextHolder.clearContext();
        }
    }

    private static void authenticate(String authority) {
        User principal = new User("actuator@example.com", "password", List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @RestController
    static class ActuatorController {

        @GetMapping("/actuator/prometheus")
        ResponseEntity<String> prometheus() {
            return ResponseEntity.ok("metrics");
        }

        @GetMapping("/actuator/health")
        ResponseEntity<String> health() {
            return ResponseEntity.ok("healthy");
        }
    }

    @EnableWebMvc
    static class TestBeans {

        @Bean
        ProblemTypeUriFactory problemTypeUriFactory() {
            return new ProblemTypeUriFactory("https://errors.example.test/problems");
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            return _ -> new CorsConfiguration();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> User.withUsername(username)
                    .password("password")
                    .authorities("USER")
                    .build();
        }
    }
}
