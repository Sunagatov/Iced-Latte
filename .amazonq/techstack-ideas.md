# Tech Stack Ideas for CV (2026)

## Priority 1 — Highest CV Impact

### Kafka
Replace `@Async` review processing with a Kafka topic.
- `review.created` → moderation consumer
- `review.deleted` → summary consumer
- Testcontainers already supports it

### pgvector + Semantic Search
Store product/review embeddings in pgvector (PostgreSQL extension), add semantic search endpoint.
- LangChain4j already handles embeddings
- "AI-powered search" is extremely hot in 2026

### RAG Pattern
LangChain4j already supports it. Let users ask "which coffee is best for espresso?" and answer from the product catalog.
- Shows full AI stack understanding, not just API calls

---

## Priority 2 — Complete the Observability Triangle

You have metrics (Prometheus) + traces (OTel) — add the third leg:

### Structured Log Aggregation
Loki + Grafana, or ELK stack. Logstash encoder is already wired, just needs the sink.
- Logs + metrics + traces = full observability triangle

### OpenTelemetry Traces to Jaeger/Tempo
Micrometer + OTel is already wired, just needs an exporter and collector.

---

## Priority 3 — Architecture Patterns

### Debezium + Kafka CDC
Stream PostgreSQL changes to Kafka via Change Data Capture.
- Shows event sourcing patterns and data pipeline knowledge, not just CRUD

### Spring Authorization Server
Replace hand-rolled JWT with a proper OAuth2 authorization server.
- Shows protocol understanding, not just library usage

### GraphQL
Add `/graphql` endpoint alongside REST for product/review queries (Spring for GraphQL).

### gRPC
Internal service communication if modules are ever split.

---

## Priority 4 — Testing & Security

### Pact Contract Testing
Consumer-driven contracts between frontend and backend.
- Very few junior/mid CVs have this, asked about at senior level

### OWASP Dependency Scanning
One Maven plugin in `pom.xml`, shows security awareness in CI pipeline.

### Chaos Engineering
Chaos Monkey for Spring Boot — basic fault injection.
- Niche but memorable in interviews

---

## Skip for This Project
- **Kubernetes** — no real value without multiple services
- **Service mesh (Istio)** — overkill for a monolith
- **WebFlux** — would require rewriting everything
