package com.zufar.icedlatte.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ClientIpExtractor unit tests")
class ClientIpExtractorTest {

    private ClientIpExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ClientIpExtractor();
    }

    @Nested
    @DisplayName("extract")
    class Extract {

        @Test
        @DisplayName("returns remote address when no trusted proxies are configured")
        void returnsRemoteAddressWhenNoTrustedProxiesAreConfigured() {
            setTrustedProxies(List.of());
            HttpServletRequest request = request("1.2.3.4", "9.9.9.9");

            assertThat(extractor.extract(request)).isEqualTo("1.2.3.4");
        }

        @Test
        @DisplayName("returns first X-Forwarded-For IP when remote address is trusted")
        void returnsFirstXffIpWhenRemoteAddressIsTrusted() {
            setTrustedProxies(List.of("10.0.0.1"));
            HttpServletRequest request = request("10.0.0.1", "5.6.7.8, 10.0.0.1");

            assertThat(extractor.extract(request)).isEqualTo("5.6.7.8");
        }

        @Test
        @DisplayName("accepts CIDR trusted proxy rule")
        void acceptsCidrTrustedProxyRule() {
            setTrustedProxies(List.of("10.0.0.0/24"));
            HttpServletRequest request = request("10.0.0.42", "5.6.7.8, 10.0.0.42");

            assertThat(extractor.extract(request)).isEqualTo("5.6.7.8");
        }

        @Test
        @DisplayName("falls back to remote address when CIDR rule does not match")
        void fallsBackToRemoteAddressWhenCidrRuleDoesNotMatch() {
            setTrustedProxies(List.of("10.0.0.0/24"));
            HttpServletRequest request = request("10.0.1.42", "5.6.7.8, 10.0.1.42");

            assertThat(extractor.extract(request)).isEqualTo("10.0.1.42");
        }

        @Test
        @DisplayName("falls back to remote address when XFF is absent")
        void fallsBackToRemoteAddressWhenXffIsAbsent() {
            setTrustedProxies(List.of("10.0.0.1"));
            HttpServletRequest request = request("10.0.0.1", null);

            assertThat(extractor.extract(request)).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("falls back to remote address when XFF contains invalid IP")
        void fallsBackToRemoteAddressWhenXffContainsInvalidIp() {
            setTrustedProxies(List.of("10.0.0.1"));
            HttpServletRequest request = request("10.0.0.1", "not-an-ip");

            assertThat(extractor.extract(request)).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("accepts valid IPv6 address in XFF")
        void acceptsValidIpv6AddressInXff() {
            setTrustedProxies(List.of("10.0.0.1"));
            HttpServletRequest request = request("10.0.0.1", "2001:db8::1");

            assertThat(extractor.extract(request)).isEqualTo("2001:db8::1");
        }

        @Test
        @DisplayName("ignores XFF when remote address is not trusted")
        void ignoresXffWhenRemoteAddressIsNotTrusted() {
            setTrustedProxies(List.of("10.0.0.1"));
            HttpServletRequest request = request("1.2.3.4", "9.9.9.9");

            assertThat(extractor.extract(request)).isEqualTo("1.2.3.4");
        }
    }

    @Nested
    @DisplayName("sanitize")
    class Sanitize {

        @Test
        @DisplayName("replaces CR and LF to avoid log injection")
        void replacesCrAndLfToAvoidLogInjection() {
            assertThat(ClientIpExtractor.sanitize("abc\r\ndef"))
                    .isEqualTo("abc__def");
        }

        @Test
        @DisplayName("returns empty string for null input")
        void returnsEmptyStringForNullInput() {
            assertThat(ClientIpExtractor.sanitize(null)).isEmpty();
        }
    }

    private void setTrustedProxies(List<String> proxies) {
        ReflectionTestUtils.setField(extractor, "trustedProxies", proxies);
    }

    private static HttpServletRequest request(String remoteAddr, String xForwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        return request;
    }
}
