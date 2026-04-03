package com.zufar.icedlatte.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("CorrelationFilter unit tests")
class CorrelationFilterTest {

    private final CorrelationFilter filter = new CorrelationFilter();

    @Test
    @DisplayName("sets X-Correlation-ID and X-Request-ID response headers")
    void setsResponseHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Correlation-ID")).isNotBlank();
        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
    }

    @Test
    @DisplayName("uses provided X-Correlation-ID header value")
    void usesProvidedCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("abc123");
    }

    @Test
    @DisplayName("sanitizes unsafe characters in headers")
    void sanitizesUnsafeHeaderChars() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "abc<script>");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("X-Correlation-ID")).doesNotContain("<").doesNotContain(">");
    }

    @Test
    @DisplayName("truncates header values longer than 64 characters")
    void truncatesLongHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "a".repeat(100));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("X-Correlation-ID")).hasSize(64);
    }
}
