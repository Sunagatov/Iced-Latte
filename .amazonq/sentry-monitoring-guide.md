# Sentry Monitoring & Performance Guide

## Overview

Iced Latte uses **Sentry 8.36.0** with Spring Boot 4 for comprehensive error tracking and performance monitoring.

## Configuration Summary

### Error Sampling

| Environment | Sample Rate | Description |
|-------------|-------------|-------------|
| **Development** | 100% (`1.0`) | Capture all errors for debugging |
| **Production** | 100% (`1.0`) | Capture all errors in production |

### Performance Monitoring (Traces)

| Environment | Trace Sample Rate | Profile Sample Rate |
|-------------|-------------------|---------------------|
| **Development** | 100% (`1.0`) | 100% (`1.0`) |
| **Production** | 5% (`0.05`) | 5% (`0.05`) |

### Intelligent Sampling by Endpoint

The application uses **dynamic sampling** based on endpoint criticality:

```java
// Critical endpoints (auth, payment, orders) → 100% sampling
/api/v1/auth/*       → 1.0 (100%)
/api/v1/payment/*    → 1.0 (100%)
/api/v1/orders/*     → 1.0 (100%)

// User-facing endpoints → 50% sampling
/api/v1/products/*   → 0.5 (50%)
/api/v1/cart/*       → 0.5 (50%)

// Everything else → 10% sampling
*                    → 0.1 (10%)
```

## Features Enabled

### ✅ Error Tracking
- Automatic exception capture
- Stack traces with source context
- Thread information
- Custom tags (application name, version)

### ✅ Performance Monitoring
- Transaction tracing
- Database query monitoring
- HTTP request tracking
- Custom performance metrics

### ✅ Profiling
- CPU profiling for traced transactions
- Method-level performance insights
- Bottleneck identification

### ✅ Privacy & Security
- PII sanitization (email, password, phone)
- Authorization header removal
- Cookie sanitization
- Request data redaction

### ✅ Filtering
- Health check endpoints excluded
- Actuator endpoints filtered
- Custom exception ignoring (e.g., `IllegalArgumentException`)

## Configuration Files

### application.yaml (Base Configuration)
```yaml
sentry:
  enabled: ${SENTRY_ENABLED:false}
  dsn: ${SENTRY_DSN:}
  environment: ${SENTRY_ENVIRONMENT:local}
  sample-rate: 1.0
  traces-sample-rate: 0.1
  profiles-sample-rate: 0.1
  enable-tracing: true
  enable-profiling: true
```

### application-dev.yaml (Development)
```yaml
sentry:
  sample-rate: 1.0
  traces-sample-rate: 1.0
  profiles-sample-rate: 1.0
  debug: true
```

### application-prod.yaml (Production)
```yaml
sentry:
  sample-rate: 1.0
  traces-sample-rate: 0.05
  profiles-sample-rate: 0.05
```

## Environment Variables

Set these in your `.env` file or deployment environment:

```bash
# Enable Sentry
SENTRY_ENABLED=true

# Your Sentry DSN (from Sentry dashboard)
SENTRY_DSN=https://your-key@o123456.ingest.sentry.io/123456

# Environment name (local, dev, staging, prod)
SENTRY_ENVIRONMENT=prod
```

## Custom Callbacks

### 1. BeforeSendCallback
Sanitizes PII and adds custom tags before sending events to Sentry.

### 2. BeforeBreadcrumbCallback
Removes sensitive data from breadcrumbs.

### 3. TracesSamplerCallback
Implements intelligent sampling based on endpoint criticality.

### 4. BeforeSendTransactionCallback
Filters out health check transactions and adds custom tags.

## Monitoring in Sentry Dashboard

### Key Metrics to Watch

1. **Error Rate**
   - Navigate to: Issues → Overview
   - Monitor: New issues, regression rate

2. **Performance**
   - Navigate to: Performance → Overview
   - Monitor: P50, P75, P95, P99 response times
   - Watch: Slow transactions, database queries

3. **Profiling**
   - Navigate: Profiling → Flame Graphs
   - Identify: CPU bottlenecks, slow methods

4. **Releases**
   - Track: Error rates per release
   - Compare: Performance across versions

## Best Practices

### ✅ DO
- Keep `sample-rate: 1.0` for errors (capture all)
- Use lower `traces-sample-rate` in production (5-10%)
- Monitor Sentry quota usage
- Set up alerts for critical errors
- Review performance trends weekly

### ❌ DON'T
- Don't set `traces-sample-rate: 1.0` in production (too expensive)
- Don't send PII to Sentry (already sanitized)
- Don't ignore all exceptions (be selective)
- Don't forget to set `SENTRY_ENVIRONMENT`

## Performance Impact

| Feature | Overhead | Recommendation |
|---------|----------|----------------|
| Error tracking | < 1ms | Always enabled |
| Tracing (5%) | ~2-5ms | Production safe |
| Tracing (100%) | ~10-20ms | Dev/staging only |
| Profiling (5%) | ~5-10ms | Production safe |
| Profiling (100%) | ~20-50ms | Dev/staging only |

## Troubleshooting

### Issue: No events in Sentry
**Solution:**
1. Check `SENTRY_ENABLED=true`
2. Verify `SENTRY_DSN` is correct
3. Check application logs for Sentry errors
4. Ensure network connectivity to Sentry

### Issue: Too many events
**Solution:**
1. Lower `traces-sample-rate` in production
2. Add more endpoints to filter list
3. Increase ignored exceptions list

### Issue: Missing performance data
**Solution:**
1. Verify `enable-tracing: true`
2. Check `traces-sample-rate > 0`
3. Ensure Spring Boot actuator is enabled

## Integration with Other Tools

### Micrometer/Prometheus
Sentry works alongside Micrometer metrics:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Separate from Sentry
```

### OpenTelemetry
Sentry can export to OpenTelemetry:
```yaml
management:
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

## Cost Optimization

### Quota Management
- **Errors**: ~1,000/month (free tier)
- **Transactions**: ~10,000/month (free tier)
- **Profiling**: Included with transactions

### Optimization Tips
1. Use intelligent sampling (already configured)
2. Filter health checks (already configured)
3. Ignore common exceptions (configure as needed)
4. Monitor quota in Sentry dashboard

## Support & Resources

- **Sentry Docs**: https://docs.sentry.io/platforms/java/guides/spring-boot/
- **GitHub Issues**: https://github.com/getsentry/sentry-java/issues
- **Community**: https://discord.gg/sentry

## Version History

| Date | Version | Changes |
|------|---------|---------|
| 2026-03-18 | 8.36.0 | Initial Spring Boot 4 setup with performance monitoring |
| 2026-03-18 | 8.36.0 | Added intelligent sampling and profiling |

---

**Last Updated**: 2026-03-18  
**Sentry Version**: 8.36.0  
**Spring Boot Version**: 4.0.3
