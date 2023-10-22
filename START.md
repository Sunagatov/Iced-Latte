### service containers:
```bash
docker compose -f docker-compose.services.yml -p online-store-services up -d
```

### app containers:
```bash
docker compose up -d --build
```

### build
```bash
docker compose build
```

### push
```bash
docker compose push
```

### restart
```bash
docker compose pull \
&& docker compose down \
&& docker compose up -d
```
