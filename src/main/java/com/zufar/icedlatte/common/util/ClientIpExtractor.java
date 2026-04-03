package com.zufar.icedlatte.common.util;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ClientIpExtractor {

    // Matches only literal IPv4 and IPv6 addresses — no hostnames, no DNS resolution.
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    // Requires at least two colon-separated groups of 1–4 hex digits, covering full, compressed,
    // and mixed IPv4-in-IPv6 forms while rejecting bare strings like ":" or "abc".
    private static final Pattern IPV6 = Pattern.compile(
            "^[0-9a-fA-F]{0,4}(:[0-9a-fA-F]{0,4}){2,7}(%[\\w.]+)?$");

    @Value("${security.trusted-proxies:}")
    private List<String> trustedProxies;

    @PostConstruct
    void logConfig() {
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            log.info("rate_limit.trusted_proxies: none configured — X-Forwarded-For will be ignored");
        } else {
            log.info("rate_limit.trusted_proxies: count={}, values={}", trustedProxies.size(), trustedProxies);
        }
    }

    public String extract(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies != null && trustedProxies.contains(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String firstIp = xForwardedFor.split(",")[0].trim();
                if (isLiteralIp(firstIp)) {
                    return firstIp;
                }
            }
        }
        return remoteAddr;
    }

    /**
     * Shared log-sanitizer: strips CR/LF to prevent log injection.
     * Used by rate-limiting filters so the logic lives in one place.
     */
    public static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n]", "_");
    }

    private static boolean isLiteralIp(String ip) {
        return IPV4.matcher(ip).matches() || IPV6.matcher(ip).matches();
    }
}
