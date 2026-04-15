# Vault — Single Source of Truth

## What is Vault
`/Users/zufar/IdeaProjects/Vault` is a **private GitHub repo** that is the single source of truth
for all Hetzner server configurations, env files, and docker-compose files.

GitHub repo: `Sunagatov/Vault` (private)

## Rule: Always edit in Vault, then push to server

When Amazon Q needs to change any of the following, it MUST edit the file in Vault first,
then push to the server — never edit directly on the server:

- Any `.env.prod` or `.env` file
- Any `docker-compose.yml` on the server
- Any `config.alloy` (Grafana Alloy)
- Any `config.yaml` (OTel Collector)

## Vault structure

```
Vault/
  Taskfile.yml                          # backup/restore/status/save tasks
  hetzner/apps/
    iced-latte/.env.prod                # iced-latte production env (source of truth)
    iced-latte/docker-compose.yml
    otel-collector/.env                 # OTel Collector credentials
    otel-collector/config.yaml          # OTel Collector pipeline config
    otel-collector/docker-compose.yml
    alloy/config.alloy                  # Grafana Alloy scrape config
    alloy/docker-compose.yml
    lexora/.env.prod
    lexora/docker-compose.yml
    festiva/.env.prod
    festiva/docker-compose.yml
    yulia-lingo/.env
    yulia-lingo/docker-compose.yml
    timetable-bot/.env.prod
    url-shortener/.env
    url-shortener/docker-compose.yml
    wg-easy/docker-compose.yml
    nginx-proxy-manager/docker-compose.yml
```

## Hetzner server

- IP: `116.203.197.65`
- SSH: `ssh -i ~/.ssh/id_rsa root@116.203.197.65`
- App root: `/opt/apps/`
- Docker network: `reverse-network`

## Workflow for Amazon Q

### To change an env var or config:
1. Edit the file in `Vault/hetzner/apps/<app>/`
2. Push to server: `cd /Users/zufar/IdeaProjects/Vault && task restore:app APP=<app>`
3. Restart the app on server if needed
4. Save to GitHub: `task save`

### To deploy iced-latte (build + push + sync + deploy):
```bash
cd /Users/zufar/IdeaProjects/Iced-Latte/maintaner
task -t Taskfile.yml prod:ship
```
The `prod:sync` step reads `.env.prod` from Vault automatically.

### To update OTel Collector config:
1. Edit `Vault/hetzner/apps/otel-collector/config.yaml`
2. Run: `cd /Users/zufar/IdeaProjects/Iced-Latte/maintaner && task -t Taskfile.yml otel:deploy`
   (reads from Vault automatically)

### To backup current server state to Vault:
```bash
cd /Users/zufar/IdeaProjects/Vault && task backup && task save
```

## Key credentials (all in Vault)

| File | Contains |
|---|---|
| `hetzner/apps/iced-latte/.env.prod` | All iced-latte prod secrets |
| `hetzner/apps/otel-collector/.env` | Grafana Cloud OTLP credentials |
| `hetzner/apps/festiva/.env.prod` | Festiva bot secrets |
| `hetzner/apps/lexora/.env.prod` | Lexora secrets |

## Do NOT

- Edit `.env.prod` directly on the server without updating Vault
- Commit secrets to the Iced-Latte public repo
- Use `maintaner/deployment/.env.prod` as the source of truth (it's a local copy/fallback only)
