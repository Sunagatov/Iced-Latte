# Multi-Vendor Logging Monitoring Guide

## Overview

Iced Latte now supports **4 major logging vendors** with a unified configuration approach:

1. **Sentry** - Error tracking and performance monitoring
2. **Grafana Loki** - Log aggregation and querying
3. **Elasticsearch/Logstash** - Full-text search and analytics
4. **Datadog** - Unified observability platform

## Supported Vendors

| Vendor | Purpose | Best For | Cost |
|--------|---------|----------|------|
| **Sentry** | Error tracking, performance | Error monitoring, APM | Free tier: 5K errors/mo |
| **Grafana Loki** | Log aggregation | Cost-effective log storage | Free (self-hosted) |
| **Elasticsearch** | Search & analytics | Complex log queries | Free (self-hosted) |
| **Datadog** | Full observability | Enterprise monitoring | $15/host/mo |

## Quick Start

### Enable All Vendors

```bash
# Sentry (already configured)
export SENTRY_ENABLED=true
export SENTRY_DSN="https://your-key@o123456.ingest.sentry.io/123456"

# Grafana Loki
export LOKI_ENABLED=true
export LOKI_URL="http://localhost:3100/loki/api/v1/push"

# Elasticsearch/Logstash
export LOGSTASH_ENABLED=true
export LOGSTASH_HOST="localhost"
export LOGSTASH_PORT="5000"

# Datadog
export DATADOG_ENABLED=true
export DATADOG_API_KEY="your-datadog-api-key"
```

### Enable Specific Vendors

```bash
# Only Sentry + Loki (recommended for cost-effectiveness)
export SENTRY_ENABLED=true
export LOKI_ENABLED=true

# Only Datadog (enterprise)
export DATADOG_ENABLED=true
```

## Vendor-Specific Setup

### 1. Sentry (Already Configured) ✅

**Purpose**: Error tracking and performance monitoring

**Configuration**:
```yaml
sentry:
  enabled: true
  dsn: ${SENTRY_DSN}
  environment: prod
```

**What It Captures**:
- ✅ Errors (ERROR level and above)
- ✅ Breadcrumbs (INFO level and above)
- ✅ Performance transactions
- ✅ Stack traces

**Dashboard**: https://sentry.io

---

### 2. Grafana Loki 🆕

**Purpose**: Cost-effective log aggregation and querying

**Why Loki?**
- ✅ Lightweight (indexes only labels, not content)
- ✅ Cost-effective (10x cheaper than Elasticsearch)
- ✅ Integrates with Grafana dashboards
- ✅ Perfect for Kubernetes environments

**Setup Options**:

#### Option A: Docker Compose (Local Development)
```yaml
# docker-compose.yml
version: '3'
services:
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml
    
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
```

Start: `docker-compose up -d loki grafana`

#### Option B: Grafana Cloud (Free Tier)
1. Sign up at https://grafana.com/auth/sign-up/create-user
2. Get your Loki endpoint: `https://logs-prod-XXX.grafana.net/loki/api/v1/push`
3. Get your API key from Grafana Cloud dashboard
4. Configure:
```bash
export LOKI_ENABLED=true
export LOKI_URL="https://logs-prod-XXX.grafana.net/loki/api/v1/push"
```

**Querying Logs in Grafana**:
```logql
# All logs from your application
{application="Iced Latte Application"}

# Only errors
{application="Iced Latte Application", level="ERROR"}

# Specific user
{application="Iced Latte Application"} |= "userId=123"

# Search for text
{application="Iced Latte Application"} |= "payment failed"
```

**Free Tier Limits**:
- 50GB logs/month
- 14 days retention
- 3 users

---

### 3. Elasticsearch/Logstash 🆕

**Purpose**: Full-text search and complex analytics

**Why Elasticsearch?**
- ✅ Powerful full-text search
- ✅ Complex aggregations and analytics
- ✅ Rich query DSL
- ✅ Kibana dashboards

**Setup Options**:

#### Option A: Docker Compose (Local Development)
```yaml
# docker-compose.yml
version: '3'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    
  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    ports:
      - "5000:5000"
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    
  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
```

**logstash.conf**:
```ruby
input {
  tcp {
    port => 5000
    codec => json
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "iced-latte-%{+YYYY.MM.dd}"
  }
}
```

Start: `docker-compose up -d elasticsearch logstash kibana`

#### Option B: Elastic Cloud (Free Trial)
1. Sign up at https://cloud.elastic.co/registration
2. Create deployment
3. Get Logstash endpoint
4. Configure:
```bash
export LOGSTASH_ENABLED=true
export LOGSTASH_HOST="your-deployment.es.io"
export LOGSTASH_PORT="9243"
```

**Querying in Kibana**:
```json
// All errors
{
  "query": {
    "match": {
      "level": "ERROR"
    }
  }
}

// Specific user
{
  "query": {
    "term": {
      "trace.userId": "user-123"
    }
  }
}

// Time range + text search
{
  "query": {
    "bool": {
      "must": [
        { "range": { "@timestamp": { "gte": "now-1h" } } },
        { "match": { "message": "payment" } }
      ]
    }
  }
}
```

**Free Trial**: 14 days, then $95/month

---

### 4. Datadog 🆕

**Purpose**: Unified observability (logs, metrics, traces, APM)

**Why Datadog?**
- ✅ All-in-one platform
- ✅ Automatic log parsing
- ✅ APM integration
- ✅ Alerting and dashboards
- ✅ Enterprise support

**Setup**:

1. Sign up at https://www.datadoghq.com/
2. Get API key from https://app.datadoghq.com/organization-settings/api-keys
3. Configure:
```bash
export DATADOG_ENABLED=true
export DATADOG_API_KEY="your-api-key"
```

**Datadog Agent (Optional but Recommended)**:
```yaml
# docker-compose.yml
version: '3'
services:
  datadog-agent:
    image: gcr.io/datadoghq/agent:latest
    environment:
      - DD_API_KEY=${DATADOG_API_KEY}
      - DD_SITE=datadoghq.com
      - DD_LOGS_ENABLED=true
      - DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true
      - DD_APM_ENABLED=true
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - /proc/:/host/proc/:ro
      - /sys/fs/cgroup/:/host/sys/fs/cgroup:ro
```

**Querying in Datadog**:
```
# All errors
status:error

# Specific service
service:iced-latte-application status:error

# User-specific
@usr.id:user-123

# Time range
@timestamp:[now-1h TO now] status:error
```

**Pricing**:
- Free trial: 14 days
- Pro: $15/host/month
- Enterprise: $23/host/month

---

## Log Format Comparison

### Console (Human-Readable)
```
2026-03-18 [http-nio-8083-exec-1] ERROR c.z.i.s.GlobalExceptionHandler traceId=abc123 spanId=def456 userId=user-789 - Payment failed
```

### File (JSON)
```json
{
  "@timestamp": "2026-03-18T01:30:00.000Z",
  "level": "ERROR",
  "logger": "com.zufar.icedlatte.security.GlobalExceptionHandler",
  "message": "Payment failed",
  "application": "Iced Latte Application",
  "version": "1",
  "environment": "prod",
  "trace": {
    "traceId": "abc123",
    "spanId": "def456",
    "userId": "user-789",
    "correlationId": "corr-123",
    "sessionId": "sess-456"
  },
  "stack_trace": "..."
}
```

### Loki (Labels + JSON)
```
Labels: {application="Iced Latte Application", environment="prod", level="ERROR"}
Message: {"level":"ERROR","logger":"...","message":"Payment failed","traceId":"abc123"}
```

### Datadog (Datadog Format)
```json
{
  "timestamp": "2026-03-18T01:30:00.000Z",
  "status": "error",
  "message": "Payment failed",
  "ddsource": "java",
  "service": "Iced Latte Application",
  "version": "1",
  "env": "prod",
  "dd.trace_id": "abc123",
  "dd.span_id": "def456",
  "usr.id": "user-789"
}
```

---

## Vendor Comparison

| Feature | Sentry | Loki | Elasticsearch | Datadog |
|---------|--------|------|---------------|---------|
| **Error Tracking** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Log Search** | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Performance** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Cost** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Setup Ease** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Dashboards** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Alerting** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Free Tier** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |

---

## Recommended Configurations

### Startup/Small Team (Free)
```bash
SENTRY_ENABLED=true          # Error tracking
LOKI_ENABLED=true            # Log aggregation
```
**Cost**: $0/month  
**Covers**: Errors, logs, basic monitoring

### Growing Team (Budget-Conscious)
```bash
SENTRY_ENABLED=true          # Error tracking
LOKI_ENABLED=true            # Log aggregation
LOGSTASH_ENABLED=true        # Advanced search (self-hosted)
```
**Cost**: ~$50/month (hosting)  
**Covers**: Errors, logs, advanced search

### Enterprise
```bash
DATADOG_ENABLED=true         # All-in-one
```
**Cost**: $15-23/host/month  
**Covers**: Everything (logs, metrics, traces, APM)

---

## Performance Impact

| Vendor | Overhead | Network | Disk |
|--------|----------|---------|------|
| **Sentry** | < 1ms | Minimal | None |
| **Loki** | ~2-5ms | Low | None |
| **Logstash** | ~5-10ms | Medium | None |
| **Datadog** | ~2-5ms | Low | None |
| **All Combined** | ~10-20ms | Medium | None |

**Recommendation**: Enable only what you need. Start with Sentry + Loki.

---

## Troubleshooting

### Issue: Logs not appearing in Loki
**Check**:
1. Loki is running: `curl http://localhost:3100/ready`
2. `LOKI_ENABLED=true` in environment
3. Check application logs for connection errors
4. Verify Loki URL is correct

### Issue: Logstash connection refused
**Check**:
1. Logstash is running: `curl http://localhost:9600`
2. Port 5000 is open
3. Firewall allows TCP connections
4. Check Logstash logs: `docker logs logstash`

### Issue: Datadog not receiving logs
**Check**:
1. API key is correct
2. `DATADOG_ENABLED=true`
3. Check Datadog agent status
4. Verify network connectivity to Datadog

### Issue: High memory usage
**Solution**:
1. Reduce async queue sizes in logback-spring.xml
2. Disable unused vendors
3. Increase log level to WARN in production

---

## Cost Optimization

### Free Tier Strategy
```bash
# Use free tiers only
SENTRY_ENABLED=true          # 5K errors/month
LOKI_ENABLED=true            # 50GB logs/month (Grafana Cloud)
```

### Self-Hosted Strategy
```bash
# Host your own (cheapest)
LOKI_ENABLED=true            # Self-hosted
LOGSTASH_ENABLED=true        # Self-hosted
```
**Cost**: ~$20/month (VPS hosting)

### Hybrid Strategy
```bash
# Critical: Paid, Non-critical: Free
SENTRY_ENABLED=true          # Paid ($26/month)
LOKI_ENABLED=true            # Free (Grafana Cloud)
```

---

## Monitoring Best Practices

### 1. Log Levels
```
ERROR   → Critical issues requiring immediate attention
WARN    → Potential issues, degraded performance
INFO    → Important business events
DEBUG   → Detailed diagnostic information (dev only)
```

### 2. Structured Logging
```java
// ✅ Good
log.info("order.created: orderId={}, userId={}, amount={}", orderId, userId, amount);

// ❌ Bad
log.info("Order " + orderId + " created by " + userId);
```

### 3. Correlation IDs
All logs include:
- `traceId` - Distributed tracing
- `spanId` - Span within trace
- `userId` - User identifier
- `correlationId` - Request correlation
- `sessionId` - User session

### 4. Sampling
- **Development**: Log everything
- **Production**: Log INFO and above
- **High-traffic**: Consider sampling DEBUG logs

---

## Integration with Existing Tools

### Prometheus/Grafana
Loki integrates seamlessly with Grafana:
```yaml
# Grafana datasource
datasources:
  - name: Loki
    type: loki
    url: http://loki:3100
```

### OpenTelemetry
Logs include OpenTelemetry trace context:
```json
{
  "traceId": "abc123",
  "spanId": "def456"
}
```

### Kubernetes
Loki is perfect for Kubernetes:
```yaml
# Promtail DaemonSet collects logs
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: promtail
spec:
  template:
    spec:
      containers:
      - name: promtail
        image: grafana/promtail:latest
```

---

## Support & Resources

### Sentry
- Docs: https://docs.sentry.io
- Community: https://discord.gg/sentry

### Grafana Loki
- Docs: https://grafana.com/docs/loki/
- Community: https://community.grafana.com

### Elasticsearch
- Docs: https://www.elastic.co/guide/
- Community: https://discuss.elastic.co

### Datadog
- Docs: https://docs.datadoghq.com
- Support: https://help.datadoghq.com

---

**Last Updated**: 2026-03-18  
**Supported Vendors**: Sentry, Grafana Loki, Elasticsearch/Logstash, Datadog  
**Spring Boot Version**: 4.0.3
