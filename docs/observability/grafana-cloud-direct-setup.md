# Direct Grafana Cloud production setup

This document describes the direct Grafana Cloud production approach for Iced Latte.

## Signals

- Logs: direct push from Logback to Grafana Cloud Loki
- Traces: direct OTLP export from the backend to Grafana Cloud OTLP
- Metrics: Grafana Cloud Metrics Endpoint integration scraping `/actuator/prometheus`

## Why

This approach avoids extra Docker collectors in production.

## Notes

- Metrics Endpoint requires a public HTTPS metrics URL and supports Basic or Bearer authentication.
- Loki4j supports `http.auth.username` and `http.auth.password` for hosted Loki services such as Grafana Cloud.
