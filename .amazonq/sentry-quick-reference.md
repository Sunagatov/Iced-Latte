# Sentry Quick Reference Card

## 🚀 Quick Start

```bash
# Enable Sentry
export SENTRY_ENABLED=true
export SENTRY_DSN="https://your-key@o123456.ingest.sentry.io/123456"
export SENTRY_ENVIRONMENT="prod"
```

## 📊 Sampling Rates

| Metric | Dev | Prod | Purpose |
|--------|-----|------|---------|
| **Errors** | 100% | 100% | Capture all errors |
| **Traces** | 100% | 5% | Performance monitoring |
| **Profiles** | 100% | 5% | CPU profiling |

## 🎯 Intelligent Sampling

```
/api/v1/auth/*     → 100% (critical)
/api/v1/payment/*  → 100% (critical)
/api/v1/orders/*   → 100% (critical)
/api/v1/products/* → 50%  (user-facing)
/api/v1/cart/*     → 50%  (user-facing)
/*                 → 10%  (everything else)
```

## 🔒 Privacy Features

✅ Email redacted  
✅ Password redacted  
✅ Phone redacted  
✅ Authorization header removed  
✅ Cookie removed  

## 🚫 Filtered Out

- Health checks (`/actuator/health`)
- Actuator endpoints
- `IllegalArgumentException`

## 📈 Key Metrics

### In Sentry Dashboard

1. **Issues** → Error rate, new issues
2. **Performance** → P50, P75, P95, P99
3. **Profiling** → CPU bottlenecks
4. **Releases** → Version comparison

## ⚙️ Configuration Files

```
application.yaml       → Base config
application-dev.yaml   → 100% sampling
application-prod.yaml  → 5% sampling
```

## 🔧 Custom Callbacks

| Callback | Purpose |
|----------|---------|
| `BeforeSendCallback` | Sanitize PII, add tags |
| `BeforeBreadcrumbCallback` | Remove sensitive breadcrumb data |
| `TracesSamplerCallback` | Intelligent endpoint sampling |
| `BeforeSendTransactionCallback` | Filter transactions, add tags |

## 📦 Dependencies

```xml
<sentry.version>8.36.0</sentry.version>

<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-4</artifactId>
</dependency>
```

## 🎛️ Toggle Features

```yaml
sentry:
  enable-tracing: true      # Performance monitoring
  enable-profiling: true    # CPU profiling
  debug: false              # Debug mode (dev only)
```

## 💰 Cost Optimization

| Tier | Errors/mo | Transactions/mo | Cost |
|------|-----------|-----------------|------|
| Free | 5,000 | 10,000 | $0 |
| Team | 50,000 | 100,000 | $26/mo |
| Business | 500,000 | 1,000,000 | $80/mo |

**Tip**: Use 5% sampling in production to stay within free tier.

## 🐛 Troubleshooting

### No events?
1. Check `SENTRY_ENABLED=true`
2. Verify `SENTRY_DSN`
3. Check logs for errors

### Too many events?
1. Lower `traces-sample-rate`
2. Add more filters
3. Ignore more exceptions

### Missing performance data?
1. Verify `enable-tracing: true`
2. Check `traces-sample-rate > 0`

## 📚 Resources

- **Guide**: `.amazonq/sentry-monitoring-guide.md`
- **Docs**: https://docs.sentry.io/platforms/java/guides/spring-boot/
- **GitHub**: https://github.com/getsentry/sentry-java

## 🏷️ Version Info

- **Sentry**: 8.36.0
- **Spring Boot**: 4.0.3
- **Last Updated**: 2026-03-18
