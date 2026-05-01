package com.zufar.icedlatte.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@DisplayName("CorrelationFilter unit tests")
class CorrelationFilterTest {

    private final CorrelationFilter filter = new CorrelationFilter();

    @Test
    @DisplayName("sets correlation and request headers on response")
    void setsCorrelationAndRequestHeadersOnResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Correlation-ID")).isNotBlank();
        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
    }

    @Test
    @DisplayName("uses provided correlation id header after sanitization")
    void usesProvidedCorrelationIdHeaderAfterSanitization() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "abc<script>");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("abc_script_");
    }

    @Test
    @DisplayName("truncates correlation id header longer than 64 characters")
    void truncatesCorrelationIdHeaderLongerThan64Characters() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "a".repeat(100));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("X-Correlation-ID")).hasSize(64);
    }

    @Test
    @DisplayName("populates MDC during filter chain and clears it afterwards")
    void populatesMdcDuringFilterChainAndClearsItAfterwards() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Session-ID", "session-1");
        request.addHeader("X-Trace-ID", "trace-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(ignored -> {
            assertThat(MDC.get("correlationId")).isNotBlank();
            assertThat(MDC.get("requestId")).isNotBlank();
            assertThat(MDC.get("sessionId")).isEqualTo("session-1");
            assertThat(MDC.get("clientTraceId")).isEqualTo("trace-1");
            MDC.put(RequestContextConstants.USER_ID_MDC_KEY, "user-1");
            assertThat(MDC.get(RequestContextConstants.USER_ID_MDC_KEY)).isEqualTo("user-1");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("sessionId")).isNull();
        assertThat(MDC.get("clientTraceId")).isNull();
        assertThat(MDC.get(RequestContextConstants.USER_ID_MDC_KEY)).isNull();
    }
}
