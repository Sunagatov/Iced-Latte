# Logging Monitoring Quick Start

## 🚀 5-Minute Setup

### Option 1: Grafana Loki (Recommended for Beginners)

**1. Start Loki + Grafana**:
```bash
docker-compose -f docker-compose.logging.yml up -d loki grafana
```

**2. Enable in your application**:
```bash
export LOKI_ENABLED=true
export LOKI_URL="http://localhost:3100/loki/api/v1/push"
```

**3. Start your application**:
```bash
mvn spring-boot:run
```

**4. View logs in Grafana**:
- Open: http://localhost:3000
- Go to: Explore → Select "Loki" datasource
- Query: `{application="Iced Latte Application"}`

**Done!** ✅

---

### Option 2: Full ELK Stack (Advanced)

**1. Start all services**:
```bash
docker-compose -f docker-compose.logging.yml up -d
```

**2. Enable in your application**:
```bash
export LOKI_ENABLED=true
export LOGSTASH_ENABLED=true
```

**3. Start your application**:
```bash
mvn spring-boot:run
```

**4. View logs**:
- **Grafana (Loki)**: http://localhost:3000
- **Kibana (Elasticsearch)**: http://localhost:5601

---

### Option 3: Sentry Only (Already Configured)

**1. Enable Sentry**:
```bash
export SENTRY_ENABLED=true
export SENTRY_DSN="your-sentry-dsn"
```

**2. Start your application**:
```bash
mvn spring-boot:run
```

**3. View errors**:
- Open: https://sentry.io

---

## 📊 What Each Tool Shows

| Tool | URL | Shows |
|------|-----|-------|
| **Grafana** | http://localhost:3000 | All logs, searchable |
| **Kibana** | http://localhost:5601 | Advanced log analytics |
| **Sentry** | https://sentry.io | Errors and performance |

---

## 🔍 Common Queries

### Grafana Loki

```logql
# All logs
{application="Iced Latte Application"}

# Only errors
{application="Iced Latte Application", level="ERROR"}

# Specific user
{application="Iced Latte Application"} |= "userId=123"

# Last hour
{application="Iced Latte Application"} [1h]

# Search text
{application="Iced Latte Application"} |= "payment"
```

### Kibana (Elasticsearch)

```
# All errors
level:ERROR

# Specific user
trace.userId:user-123

# Time range
@timestamp:[now-1h TO now] AND level:ERROR

# Text search
message:"payment failed"
```

---

## 🛠️ Troubleshooting

### Logs not appearing in Loki?

**Check**:
```bash
# Is Loki running?
curl http://localhost:3100/ready

# Check application logs
tail -f logs/iced-latte.log

# Verify environment variable
echo $LOKI_ENABLED
```

### Logs not appearing in Elasticsearch?

**Check**:
```bash
# Is Elasticsearch running?
curl http://localhost:9200/_cluster/health

# Is Logstash running?
curl http://localhost:9600

# Check Logstash logs
docker logs iced-latte-logstash
```

### High memory usage?

**Solution**:
```bash
# Stop unused services
docker-compose -f docker-compose.logging.yml stop elasticsearch logstash kibana

# Keep only Loki (lightweight)
docker-compose -f docker-compose.logging.yml up -d loki grafana
```

---

## 💰 Cost Comparison

| Setup | Monthly Cost | Best For |
|-------|--------------|----------|
| **Loki (self-hosted)** | $0 | Startups, small teams |
| **Loki (Grafana Cloud)** | $0 (50GB free) | Growing teams |
| **ELK (self-hosted)** | ~$50 (hosting) | Advanced analytics |
| **Datadog** | $15/host | Enterprise |

---

## 📚 Next Steps

1. **Set up alerts** in Grafana for errors
2. **Create dashboards** for key metrics
3. **Configure retention** policies
4. **Add more labels** for better filtering

---

## 🆘 Need Help?

- **Documentation**: `.amazonq/logging-monitoring-guide.md`
- **Sentry Guide**: `.amazonq/sentry-monitoring-guide.md`
- **Issues**: https://github.com/Sunagatov/Iced-Latte/issues

---

**Last Updated**: 2026-03-18  
**Recommended**: Start with Loki (Option 1)
