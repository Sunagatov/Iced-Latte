package com.zufar.icedlatte.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ClientIpExtractor Tests")
class ClientIpExtractorTest {

    private ClientIpExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ClientIpExtractor();
    }

    private void setTrustedProxies(List<String> proxies) {
        ReflectionTestUtils.setField(extractor, "trustedProxies", proxies);
    }

    @Test
    @DisplayName("returns remoteAddr when no trusted proxies configured")
    void noTrustedProxiesReturnsRemoteAddr() {
        setTrustedProxies(List.of());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9");

        assertThat(extractor.extract(request)).isEqualTo("1.2.3.4");
    }

    @Test
    @DisplayName("returns first XFF IP when remoteAddr is a trusted proxy")
    void trustedProxyReturnsFirstXffIp() {
        setTrustedProxies(List.of("10.0.0.1"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("5.6.7.8, 10.0.0.1");

        assertThat(extractor.extract(request)).isEqualTo("5.6.7.8");
    }

    @Test
    @DisplayName("falls back to remoteAddr when XFF header is absent")
    void trustedProxyNoXffHeaderReturnsRemoteAddr() {
        setTrustedProxies(List.of("10.0.0.1"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        assertThat(extractor.extract(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("falls back to remoteAddr when XFF contains an invalid IP")
    void trustedProxyInvalidXffIpReturnsRemoteAddr() {
        setTrustedProxies(List.of("10.0.0.1"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip");

        assertThat(extractor.extract(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("falls back to remoteAddr when XFF contains a hostname (no DNS resolution)")
    void trustedProxyHostnameInXffReturnsRemoteAddr() {
        setTrustedProxies(List.of("10.0.0.1"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("evil.attacker.com");

        assertThat(extractor.extract(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("accepts valid IPv6 address in XFF")
    void trustedProxyValidIpv6InXffReturnsIpv6() {
        setTrustedProxies(List.of("10.0.0.1"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8::1");

        assertThat(extractor.extract(request)).isEqualTo("2001:db8::1");
    }

    @Test
    @DisplayName("falls back to remoteAddr when XFF contains malformed IPv6 (bare colon)")
    void trustedProxyMalformedIpv6InXffReturnsRemoteAddr() {
        setTrustedProxies(List.of("10.0.0.1"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(":");

        assertThat(extractor.extract(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("ignores XFF when remoteAddr is not in trusted proxies list")
    void untrustedRemoteAddrIgnoresXff() {
        setTrustedProxies(List.of("10.0.0.1"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9");

        assertThat(extractor.extract(request)).isEqualTo("1.2.3.4");
    }
}
