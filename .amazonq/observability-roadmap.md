# Observability Roadmap 2026

## Цель
Реализовать взрослую систему мониторинга с возможностью переключения между провайдерами для метрик, логов, трейсов и APM.

---

## Провайдеры для интеграции

### 1. **Datadog** 🔥
**Что даёт:**
- APM (Application Performance Monitoring) — автоматический трейсинг всех HTTP-запросов, SQL-запросов, Redis-операций
- Distributed tracing — визуализация цепочек вызовов между сервисами
- Logs — централизованное хранилище логов с мощным поиском и фильтрацией
- Metrics — кастомные метрики + автоматические метрики JVM, Spring Boot, PostgreSQL, Redis
- Real User Monitoring (RUM) — мониторинг фронтенда (уже есть модуль `telemetry/`)
- Dashboards — готовые дашборды для Spring Boot, PostgreSQL, Redis
- Alerts — умные алерты с ML-детекцией аномалий

**Free tier:**
- 14 дней trial с полным функционалом
- Потом платно от $15/host/month

**Интеграция:**
- Micrometer registry: `micrometer-registry-datadog`
- OpenTelemetry exporter: `opentelemetry-exporter-otlp` → Datadog Agent
- Logback appender: `logstash-logback-encoder` → Datadog Agent (уже есть)

**Зачем:**
- Самый популярный APM в индустрии
- Лучшая визуализация трейсов
- Автоматическая корреляция логов ↔ трейсов ↔ метрик

---

### 2. **Grafana Cloud** (Grafana + Loki + Tempo + Mimir) 🆓
**Что даёт:**
- Grafana — визуализация метрик и дашборды
- Loki — логи (как Elasticsearch, но дешевле и проще)
- Tempo — distributed tracing
- Mimir — long-term storage для Prometheus метрик
- Alerts — алерты через Grafana Alerting

**Free tier:**
- 10K series метрик
- 50GB логов/месяц
- 50GB трейсов/месяц
- 3 пользователя
- **Навсегда бесплатно** — идеально для pet-проектов

**Интеграция:**
- Prometheus → Grafana Cloud (push через remote_write)
- Logback → Loki (через `loki-logback-appender`)
- OpenTelemetry → Tempo (OTLP endpoint)

**Зачем:**
- Бесплатный tier достаточен для production pet-проекта
- Open-source стек — можно self-host если нужно
- Grafana — индустриальный стандарт для дашбордов

---

### 3. **New Relic** 🚀
**Что даёт:**
- APM — автоматический трейсинг + code-level visibility
- Infrastructure monitoring — мониторинг хоста, Docker, Kubernetes
- Logs — централизованное хранилище с корреляцией к трейсам
- Browser monitoring — RUM для фронтенда
- Synthetic monitoring — проверка доступности API извне
- AI-powered insights — автоматическое обнаружение проблем

**Free tier:**
- 100GB данных/месяц (метрики + логи + трейсы)
- 1 пользователь
- **Навсегда бесплатно**

**Интеграция:**
- New Relic Java Agent — автоматическая инструментация (zero code changes)
- Micrometer registry: `micrometer-registry-new-relic`
- OpenTelemetry → New Relic OTLP endpoint

**Зачем:**
- Самая простая интеграция — Java Agent делает всё автоматически
- Щедрый free tier
- Отличный UI для анализа performance bottlenecks

---

### 4. **AWS CloudWatch** ☁️
**Что даёт:**
- Logs — CloudWatch Logs (интеграция с Lambda, ECS, EC2)
- Metrics — CloudWatch Metrics (кастомные + AWS-сервисы)
- Traces — AWS X-Ray (distributed tracing)
- Dashboards — CloudWatch Dashboards
- Alarms — CloudWatch Alarms

**Free tier:**
- 5GB логов/месяц
- 10 кастомных метрик
- 1M API requests
- 100K трейсов/месяц (X-Ray)

**Интеграция:**
- Logback → CloudWatch Logs (через `aws-java-sdk-logs`)
- Micrometer → CloudWatch Metrics (через `micrometer-registry-cloudwatch`)
- OpenTelemetry → AWS X-Ray (через `aws-opentelemetry-exporter-trace`)

**Зачем:**
- Нативная интеграция с AWS (проект уже использует S3)
- Если в будущем мигрируем на ECS/EKS — всё уже готово
- Бесплатно в рамках AWS Free Tier

---

### 5. **Elastic Stack** (Elasticsearch + Kibana + APM) 🔍
**Что даёт:**
- Elasticsearch — хранилище логов + full-text search
- Kibana — визуализация логов и метрик
- Elastic APM — distributed tracing + performance monitoring
- Beats — агенты для сбора метрик (Filebeat, Metricbeat)

**Free tier:**
- Elastic Cloud: 14 дней trial, потом платно
- Self-hosted: бесплатно, но нужен сервер

**Интеграция:**
- Logback → Elasticsearch (через `logstash-logback-encoder` + Logstash)
- Elastic APM Java Agent — автоматическая инструментация
- OpenTelemetry → Elastic APM

**Зачем:**
- Мощный поиск по логам (full-text search)
- Open-source — можно self-host
- Популярен в enterprise

---

### 6. **Honeycomb** 🍯
**Что даёт:**
- Observability-first подход — не метрики, а события (events)
- High-cardinality data — можно фильтровать по любым полям (userId, productId, etc.)
- BubbleUp — автоматическое обнаружение причин проблем
- Distributed tracing

**Free tier:**
- 20M events/месяц
- 60 дней retention
- **Навсегда бесплатно**

**Интеграция:**
- OpenTelemetry → Honeycomb OTLP endpoint

**Зачем:**
- Современный подход к observability (events > metrics)
- Отлично для debugging production issues
- Щедрый free tier

---

### 7. **Sentry** 🐛
**Что даёт:**
- Error tracking — автоматический захват исключений
- Performance monitoring — трейсинг медленных запросов
- Release tracking — связь ошибок с версиями приложения
- User feedback — пользователи могут отправлять feedback прямо из UI

**Free tier:**
- 5K errors/месяц
- 10K performance transactions/месяц
- **Навсегда бесплатно**

**Интеграция:**
- Sentry Java SDK — автоматический захват исключений
- Spring Boot integration — `sentry-spring-boot-starter`

**Зачем:**
- Лучший инструмент для error tracking
- Автоматическая группировка похожих ошибок
- Source maps — показывает строку кода где произошла ошибка

---

### 8. **Prometheus + Grafana** (Self-hosted) 🏠
**Что даёт:**
- Prometheus — метрики (pull-based)
- Grafana — дашборды
- Alertmanager — алерты

**Free tier:**
- Полностью бесплатно (self-hosted)

**Интеграция:**
- Уже есть: `micrometer-registry-prometheus` + `/actuator/prometheus` endpoint
- Нужно только поднять Prometheus + Grafana в Docker

**Зачем:**
- Полный контроль над данными
- Бесплатно
- Индустриальный стандарт

---

## Рекомендуемый стек для Iced Latte

### Вариант 1: Максимально бесплатный
```
Метрики:  Grafana Cloud (Prometheus + Mimir)
Логи:     Grafana Cloud (Loki)
Трейсы:   Grafana Cloud (Tempo)
Errors:   Sentry (free tier)
```

### Вариант 2: Best-in-class (платный)
```
APM:      Datadog (всё в одном)
Errors:   Sentry (специализация на errors)
```

### Вариант 3: AWS-native
```
Метрики:  CloudWatch Metrics
Логи:     CloudWatch Logs
Трейсы:   AWS X-Ray
```

### Вариант 4: Self-hosted
```
Метрики:  Prometheus
Логи:     Loki
Трейсы:   Tempo
UI:       Grafana
```

---

## Что нужно реализовать

### 1. Архитектура
- [ ] Создать модуль `observability/` с абстракцией провайдеров
- [ ] Enum `ObservabilityProvider` (DATADOG, GRAFANA, NEW_RELIC, AWS, PROMETHEUS)
- [ ] Configuration classes для каждого провайдера
- [ ] Conditional beans через `@ConditionalOnProperty`

### 2. Метрики
- [ ] Кастомные метрики: `cart.items.added`, `order.created`, `review.moderated`
- [ ] Business metrics: revenue, active users, conversion rate
- [ ] JVM metrics (уже есть через Micrometer)

### 3. Логи
- [ ] Structured logging (JSON) — уже есть через `logstash-logback-encoder`
- [ ] Correlation IDs — уже есть через `CorrelationIdFilter`
- [ ] Log shipping к провайдерам

### 4. Трейсы
- [ ] Distributed tracing через OpenTelemetry
- [ ] Автоматическая инструментация HTTP, SQL, Redis
- [ ] Кастомные spans для бизнес-логики

### 5. Dashboards
- [ ] Dashboard для каждого модуля (cart, order, review, etc.)
- [ ] SLI/SLO дашборды (latency, error rate, throughput)
- [ ] Business metrics дашборды

### 6. Alerts
- [ ] High error rate (>5%)
- [ ] Slow queries (>200ms)
- [ ] High memory usage (>80%)
- [ ] Redis connection failures

---

## Прогресс

### ✅ Уже есть
- Micrometer + Prometheus endpoint (`/actuator/prometheus`)
- OpenTelemetry tracing bridge
- Structured logging (JSON) через Logstash encoder
- Correlation IDs
- Slow query monitoring (SlowQueryAspect)

### 🚧 В процессе
- **Sentry integration** — error tracking + performance monitoring

### ⏳ Планируется
- Grafana Cloud (метрики + логи + трейсы)
- New Relic (APM)
- AWS CloudWatch (AWS-native)

---

## Sentry Implementation Plan

### Шаг 1: Добавить зависимости ✅
```xml
<sentry.version>8.0.0</sentry.version>

<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>${sentry.version}</version>
</dependency>
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-logback</artifactId>
    <version>${sentry.version}</version>
</dependency>
```
**Статус:** ✅ Готово — добавлено в `pom.xml`

### Шаг 2: Конфигурация ✅
- ✅ Добавлено `sentry.enabled`, `sentry.dsn`, `sentry.environment` в `application.yaml`
- ✅ Добавлено `SENTRY_ENABLED`, `SENTRY_DSN` в `.env`
- ✅ Настроен Logback appender для отправки ERROR логов в Sentry
- ✅ Включён performance monitoring (`traces-sample-rate: 0.1`)

### Шаг 3: Кастомизация ✅
- ✅ `SentryConfiguration` — BeforeSendCallback для фильтрации PII (email, password, phone)
- ✅ `SentryUserContextFilter` — автоматическая установка userId из SecurityPrincipalProvider
- ✅ `SentryService` — helper для breadcrumbs и custom events
- ⏳ Custom tags: `userId`, `module`, `environment` — частично (userId через filter)
- ⏳ Breadcrumbs — API готово, нужно добавить в бизнес-логику
- ⏳ Release tracking — нужно добавить в CI/CD

### Шаг 4: Тестирование
- [ ] Добавить `sentry.enabled`, `sentry.dsn`, `sentry.environment` в `application.yaml`
- [ ] Добавить `SENTRY_ENABLED`, `SENTRY_DSN` в `.env`
- [ ] Настроить Logback appender для отправки ERROR логов в Sentry
- [ ] Включить performance monitoring (трейсинг HTTP-запросов)

### Шаг 3: Кастомизация
- [ ] BeforeSendCallback — фильтрация PII (email, phone)
- [ ] Custom tags: `userId`, `module`, `environment`
- [ ] Breadcrumbs — логирование важных событий (cart.items.added, order.created)
- [ ] Release tracking — связь ошибок с версией приложения

### Шаг 4: Тестирование ✅
- ✅ Создан тестовый controller `/api/v1/observability/test/sentry/*`
  - `GET /sentry/error` — простое исключение
  - `GET /sentry/message?level=WARNING` — кастомное сообщение
  - `GET /sentry/breadcrumbs` — ошибка с breadcrumbs trail
  - `GET /sentry/nested-error` — вложенное исключение
- ✅ DSN получен и добавлен в `.env`
- ⏳ Запустить приложение и проверить захват исключений
- ⏳ Проверить performance traces
- ⏳ Проверить breadcrumbs

### Шаг 5: Production setup
- [ ] Создать проект в Sentry.io
- [ ] Получить DSN
- [ ] Настроить alerts в Sentry UI
- [ ] Настроить issue assignment rules

---

## Следующие шаги
1. ✅ Выбран Sentry для начала
2. 🚧 Добавить зависимости в pom.xml
3. ⏳ Настроить конфигурацию
4. ⏳ Протестировать интеграцию
5. ⏳ Задеплоить и проверить в production
