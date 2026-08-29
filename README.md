# backend-test

Базовое backend-приложение для тестирования и автоматизации.

## Стек

- Java 21, Spring Boot 3.4, Maven
- PostgreSQL 16 + Flyway, Spring Data JPA
- Redis (кэш, rate limit)
- Apache Kafka (события, DLT), RabbitMQ (очередь уведомлений, DLQ)
- REST, GraphQL, SOAP, WebSocket, SSE, webhooks
- JWT + BCrypt, OAuth 2.0 client_credentials
- Resilience4j (retry, circuit breaker)
- Actuator + Prometheus + Grafana, OpenTelemetry + Jaeger
- OpenAPI (Swagger UI), AsyncAPI
- WireMock как внешняя зависимость
- Docker / Docker Compose

## Запуск

```bash
docker compose up -d --build
```

Приложение из IDE, инфраструктура в Docker:

```bash
docker compose up -d postgres redis kafka rabbitmq wiremock jaeger
mvn spring-boot:run
```

## Адреса

| | |
|---|---|
| REST API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI | http://localhost:8080/v3/api-docs |
| GraphQL / GraphiQL | http://localhost:8080/graphql · /graphiql |
| SOAP WSDL | http://localhost:8080/soap/orders.wsdl |
| WebSocket | ws://localhost:8080/ws/orders |
| SSE | http://localhost:8080/api/v1/orders/stream |
| Actuator | http://localhost:8080/actuator |
| RabbitMQ UI | http://localhost:15672 (guest / guest) |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) |
| Jaeger | http://localhost:16686 |
| WireMock | http://localhost:8081/__admin |
| Kafka | localhost:9092 |
| PostgreSQL | localhost:5432 (backendtest / backendtest) |

## Эндпоинты

| Метод | Путь | Токен |
|---|---|---|
| GET | `/api/health` | нет |
| POST | `/api/v1/auth/register` · `/login` | нет |
| POST | `/api/v1/auth/token` (client_credentials, form) | нет |
| GET | `/api/v1/auth/me` | да |
| GET | `/api/v1/users` · `/{id}` | да |
| POST · DELETE | `/api/v1/users` · `/{id}` | ADMIN |
| PUT · PATCH | `/api/v1/users/{id}` | владелец или ADMIN |
| GET | `/api/v2/users` · `/{id}` | да |
| GET | `/api/v1/orders` · `/{id}` | да |
| POST | `/api/v1/orders` | да |
| PATCH | `/api/v1/orders/{id}` | владелец или ADMIN |
| POST | `/api/v1/orders/{id}/pay` · `/cancel` | владелец или ADMIN |
| POST | `/api/v1/orders/{id}/ship` · `/deliver` · `/poison` | ADMIN |
| GET | `/api/v1/orders/stream` · `/notifications` · `/subscribers` | да |
| GET · POST · DELETE | `/api/v1/webhooks` | да |
| GET | `/api/v1/shipping/quote` | нет |
| GET | `/api/v1/chaos/slow` · `/status` · `/flaky` · `/payload` | нет |

Коллекции поддерживают `?page=&size=&sort=field,dir`, пользователи — `?name=`, заказы — `?status=`.

Заголовки: `Idempotency-Key` на `POST /orders`, `If-Match` на `PATCH /orders/{id}`,
`X-Correlation-Id` на любом запросе, `X-RateLimit-*` и `Retry-After` в ответах.

Статусы заказа: `NEW → PAID → SHIPPED → DELIVERED`, отмена из `NEW` и `PAID`.
Недопустимый переход — `409`.

## События

Контракт: [`asyncapi.yaml`](asyncapi.yaml).

| | |
|---|---|
| Kafka | `orders.events` → `orders.events.DLT` (2 ретрая) |
| RabbitMQ | `notifications.queue` → `notifications.queue.dlq` |

`POST /api/v1/orders/{id}/poison` публикует событие, которое оба консьюмера отклоняют.

## Тестовые данные

Создаются при первом старте на пустой БД.

| Email | Пароль | Роль |
|---|---|---|
| admin@test.com | admin123 | ADMIN |
| alice@test.com | alice123 | USER |
| bob@test.com | bob12345 | USER |
| carol@test.com | carol123 | USER |

OAuth-клиент: `test-client` / `test-secret`.

## Переменные окружения

| Переменная | По умолчанию |
|---|---|
| `PORT` | 8080 |
| `JWT_SECRET` | dev-значение, задать своё на VPS (мин. 32 символа) |
| `JWT_TTL_MINUTES` | 60 |
| `OAUTH_CLIENT_ID` · `OAUTH_CLIENT_SECRET` | test-client · test-secret |
| `RATE_LIMIT_REQUESTS` · `RATE_LIMIT_WINDOW_SECONDS` | 100 · 60 |
| `CACHE_TTL_SECONDS` | 60 |
| `HTTP_CONNECT_TIMEOUT_MS` · `HTTP_READ_TIMEOUT_MS` | 2000 · 3000 |
| `DB_URL` · `DB_USER` · `DB_PASSWORD` | postgres в compose |
| `REDIS_HOST` · `KAFKA_BOOTSTRAP_SERVERS` · `RABBITMQ_HOST` | сервисы в compose |
| `SHIPPING_BASE_URL` | http://wiremock:8080 |
| `OTLP_ENDPOINT` | http://jaeger:4318/v1/traces |

Полному стеку нужно ~4 ГБ RAM.
