package com.zufar.icedlatte.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("ActuatorPrometheusScrapeTokenFilter")
class ActuatorPrometheusScrapeTokenFilterTest {

    private final ActuatorPrometheusScrapeTokenFilter filter =
            new ActuatorPrometheusScrapeTokenFilter("test-scrape-token");

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("clears authentication after a successful scrape request")
    void clearsAuthenticationAfterSuccessfulScrapeRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("Authorization", "Metrics test-scrape-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            Assertions.assertThat(servletRequest).isSameAs(request);
            Assertions.assertThat(servletResponse).isSameAs(response);
            assertThat(currentAuthentication()).isNotNull();
        };

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
    }

    @Test
    @DisplayName("clears authentication when downstream handling fails")
    void clearsAuthenticationWhenDownstreamHandlingFails() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("Authorization", "Metrics test-scrape-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class)
                .hasMessage("boom");

        verify(chain).doFilter(request, response);
        assertThat(currentAuthentication()).isNull();
    }

    @Test
    @DisplayName("matches prometheus path when the app is deployed under a context path")
    void matchesPrometheusPathWhenAppIsDeployedUnderContextPath() {
        TestableActuatorPrometheusScrapeTokenFilter filter =
                new TestableActuatorPrometheusScrapeTokenFilter("test-scrape-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/actuator/prometheus");
        request.setContextPath("/app");
        request.setServletPath("/actuator/prometheus");

        assertThat(filter.shouldSkip(request)).isFalse();
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static final class TestableActuatorPrometheusScrapeTokenFilter extends ActuatorPrometheusScrapeTokenFilter {

        private TestableActuatorPrometheusScrapeTokenFilter(String scrapeToken) {
            super(scrapeToken);
        }

        private boolean shouldSkip(MockHttpServletRequest request) {
            return super.shouldNotFilter(request);
        }
    }
}
