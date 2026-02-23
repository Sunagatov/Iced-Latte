package com.zufar.icedlatte.telemetry.endpoint;

import com.zufar.icedlatte.telemetry.dto.PerformanceReportDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryEndpoint {

    @PostMapping("/performance")
    public ResponseEntity<Void> recordPerformance(
            @RequestBody(required = false) PerformanceReportDto report,
            HttpServletRequest request) {

        if (report == null) return ResponseEntity.badRequest().build();

        String sessionId = nullSafe(request.getHeader("X-Session-ID"));
        String traceId = nullSafe(request.getHeader("X-Trace-ID"));

        log.info("perf.report: page={}, pageLoadMs={}, apiCalls={}, errorCount={}, p95DurationMs={}, sessionId={}, traceId={}",
                report.page(), report.pageLoadMs(),
                report.apiCalls() != null ? report.apiCalls().size() : 0,
                report.errorCount(), report.p95DurationMs(),
                sessionId, traceId);

        if (report.errorCount() > 0) {
            log.warn("perf.error_spike: page={}, errorCount={}, sessionId={}, traceId={}",
                    report.page(), report.errorCount(), sessionId, traceId);
        }

        if (report.apiCalls() != null) {
            for (var call : report.apiCalls()) {
                log.info("perf.api_call: url={}, method={}, status={}, durationMs={}, sessionId={}, traceId={}",
                        call.url(), call.method(), call.status(), call.durationMs(), sessionId, traceId);
            }
        }

        return ResponseEntity.ok().build();
    }

    private String nullSafe(String value) {
        return value != null ? value : "unknown";
    }
}
