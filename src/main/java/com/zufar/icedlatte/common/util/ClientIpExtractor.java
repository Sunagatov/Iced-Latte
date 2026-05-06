package com.zufar.icedlatte.common.util;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ClientIpExtractor {

    // Matches only literal IPv4 addresses — no hostnames, no DNS resolution.
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

    // Requires at least two colon-separated groups of 1–4 hex digits, covering full, compressed,
    // and mixed IPv4-in-IPv6 forms while rejecting bare strings like ":" or "abc".
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F]{0,4}(:[0-9a-fA-F]{0,4}){2,7}(%[\\w.]+)?$");

    @Value("${security.rate-limit.trusted-proxies:${security.trusted-proxies:}}")
    private List<String> trustedProxies;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @PostConstruct
    void logConfig() {
        trustedProxies = trustedProxies == null
                ? List.of()
                : trustedProxies.stream()
                  .map(String::trim)
                  .filter(value -> !value.isBlank())
                  .toList();

        if (trustedProxies.isEmpty()) {
            log.info("rate_limit.trusted_proxies: none configured — X-Forwarded-For will be ignored");
            if (!activeProfiles.contains("dev") && (serverPort == 8083 || serverPort == 8080)) {
                log.warn("rate_limit.proxy_hint: If this app runs behind a reverse proxy or load balancer, "
                        + "set security.rate-limit.trusted-proxies to the proxy IP(s). "
                        + "Without it, all users share a single rate-limit counter (the proxy's IP).");
            }
        } else {
            log.info("rate_limit.trusted_proxies: count={}, values={}", trustedProxies.size(), trustedProxies);
        }
    }

    public String extract(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return remoteAddress;
        }

        String firstIp = xForwardedFor.split(",")[0].trim();
        if (isLiteralIp(firstIp)) {
            return firstIp;
        }

        return remoteAddress;
    }

    /**
     * Shared log-sanitizer: strips CR/LF to prevent log injection.
     * Used by rate-limiting filters so the logic lives in one place.
     */
    public static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }

    private boolean isTrustedProxy(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }

        return trustedProxies.stream()
                .anyMatch(rule -> matchesTrustedProxyRule(remoteAddress, rule));
    }

    private static boolean matchesTrustedProxyRule(String remoteAddress, String rule) {
        if (rule == null || rule.isBlank()) {
            return false;
        }

        String normalizedRule = rule.trim();
        if (!normalizedRule.contains("/")) {
            return normalizedRule.equals(remoteAddress);
        }

        return isInCidr(remoteAddress, normalizedRule);
    }

    private static boolean isInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/", 2);
            if (parts.length != 2) {
                return false;
            }

            InetAddress ipAddress = InetAddress.getByName(ip);
            InetAddress networkAddress = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] ipBytes = ipAddress.getAddress();
            byte[] networkBytes = networkAddress.getAddress();

            if (ipBytes.length != networkBytes.length) {
                return false;
            }

            int maxPrefixLength = ipBytes.length * 8;
            if (prefixLength < 0 || prefixLength > maxPrefixLength) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = (-1) << (8 - remainingBits);
            return (ipBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isLiteralIp(String ip) {
        return IPV4.matcher(ip).matches() || IPV6.matcher(ip).matches();
    }
}