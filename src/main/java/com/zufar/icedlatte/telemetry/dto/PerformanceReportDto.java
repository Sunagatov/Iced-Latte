package com.zufar.icedlatte.telemetry.dto;

import java.util.List;

public record PerformanceReportDto(
        String page,
        Long pageLoadMs,
        int errorCount,
        Long p95DurationMs,
        List<ApiCallMetric> apiCalls
) {
    public record ApiCallMetric(
            String url,
            String method,
            int status,
            long durationMs
    ) {}
}
