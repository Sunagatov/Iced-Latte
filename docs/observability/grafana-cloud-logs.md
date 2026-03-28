# Grafana Cloud Logs for Iced Latte Backend

This setup gives you a free-cloud path for backend log inspection without changing the existing application logging code.

## Why this approach

The backend already writes structured JSON logs to `/app/logs/iced-latte.log` through Logback.
Instead of making the application push directly to a hosted Loki endpoint, this setup keeps the app unchanged and uses Promtail as the shipping layer.

That gives you a cleaner split:
- Spring Boot keeps producing logs locally
- Promtail tails the log files
- Grafana Cloud Loki stores and indexes them
- Grafana Cloud UI is used to search and inspect them

## Files added by this PR

- `docker-compose.cloud-logs.yml` — Docker Compose overlay for cloud log shipping
- `promtail/config.cloud.yml` — Promtail config for Grafana Cloud Loki
- `.env.observability.example` — non-secret template for Grafana Cloud credentials

## 1. Create Grafana Cloud credentials

Inside Grafana Cloud, create or copy the following values:
- Loki push URL
- Loki username
- Access policy token with `logs:write`

## 2. Create local env file on the server

Copy the example file:

```bash
cp .env.observability.example .env.observability
```

Then fill in the real values.

Example:

```env
OBSERVABILITY_APPLICATION=iced-latte-backend
OBSERVABILITY_ENVIRONMENT=prod
GRAFANA_CLOUD_LOKI_URL=https://logs-prod-your-region.grafana.net/loki/api/v1/push
GRAFANA_CLOUD_LOKI_USERNAME=your-grafana-cloud-loki-username
GRAFANA_CLOUD_LOKI_TOKEN=your-grafana-cloud-token
```

Do not commit the real `.env.observability` file.

## 3. Start backend with cloud log shipping

If the backend is already defined in your main compose file, run:

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.cloud-logs.yml \
  --profile backend \
  --profile cloud-logs \
  up -d
```

This does two things:
- mounts `./logs` from the host into `/app/logs` in the backend container
- starts `promtail-cloud`, which ships those logs to Grafana Cloud

## 4. Verify locally on the server

Check Promtail logs:

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.cloud-logs.yml \
  logs -f promtail-cloud
```

Check that backend log files exist on the host:

```bash
ls -lah logs/
```

You should see files like:
- `iced-latte.log`
- `iced-latte-YYYY-MM-DD.0.log`

## 5. Example LogQL queries in Grafana Cloud

Show all backend logs:

```logql
{application="iced-latte-backend"}
```

Production backend logs only:

```logql
{application="iced-latte-backend", environment="prod"}
```

Only errors:

```logql
{application="iced-latte-backend", environment="prod"} |= "ERROR"
```

Search by correlation id field content:

```logql
{application="iced-latte-backend", environment="prod"} |= "correlationId"
```

## Notes

- This setup intentionally uses file logs because your backend already writes structured JSON logs there.
- This PR does not replace your existing local Loki / Grafana playground. It adds a separate hosted-cloud path.
- This PR also does not force Grafana Cloud usage in local development. The overlay is optional.
