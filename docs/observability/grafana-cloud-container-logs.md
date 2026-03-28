# Grafana Cloud Container Logs for Iced Latte

Use this variant when you want Grafana Cloud to receive Docker container logs directly, including the frontend container.

## What this covers

This overlay is aimed at containers such as:
- `iced-latte-backend`
- `iced-latte-frontend`
- supporting Docker services that you may want to inspect later

Promtail discovers containers through the Docker socket and pushes their stdout/stderr logs to Grafana Cloud Loki.

## Files added by this PR

- `docker-compose.cloud-logs.containers.yml`
- `promtail/config.cloud.containers.yml`
- `.env.observability.example`

## Start it

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.cloud-logs.containers.yml \
  --profile backend \
  --profile frontend \
  --profile cloud-logs \
  up -d
```

If you only need the collector sidecar and your app containers are already running, use the same command without rebuilding the existing app containers.

## Example LogQL queries

Frontend container logs:

```logql
{compose_service="frontend"}
```

Backend container logs:

```logql
{compose_service="backend"}
```

Everything from the compose project:

```logql
{compose_project="iced-latte"}
```

## Notes

- This path is the easiest way to include frontend logs because the frontend already logs to container stdout.
- The file-log overlay is still better for structured backend file logs.
- You can run both overlays if you want file logs for backend and container logs for frontend.
