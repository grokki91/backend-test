# backend-test

Базовое backend-приложение для тестирования и автоматизации.

## Стек

- Java 21
- Spring Boot 3.4 (Web, Data JPA, Validation)
- H2 (файловая БД)
- JWT (jjwt) + BCrypt
- springdoc-openapi (Swagger UI)
- Maven
- Docker / Docker Compose

## Запуск

Локально:

```bash
mvn clean package
java -jar target/backend-test-1.0.0.jar
```

В Docker:

```bash
docker compose up -d --build
```

## Адреса

| | |
|---|---|
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI | http://localhost:8080/v3/api-docs |
| H2 console | http://localhost:8080/h2-console (`jdbc:h2:file:./data/testdb`, `sa` / пусто) |

## Эндпоинты

| Метод | Путь | Токен |
|---|---|---|
| GET | `/api/health` | нет |
| POST | `/api/auth/register` | нет |
| POST | `/api/auth/login` | нет |
| GET | `/api/auth/me` | да |
| GET | `/api/users` | да |
| GET | `/api/users/{id}` | да |
| POST | `/api/users` | да |
| PUT | `/api/users/{id}` | да |
| DELETE | `/api/users/{id}` | да, роль `ADMIN` |

`GET /api/users` поддерживает `?page=&size=&sort=name,desc&name=`.

## Тестовые данные

Создаются при первом старте (пустая БД):

| Email | Пароль | Роль |
|---|---|---|
| admin@test.com | admin123 | ADMIN |
| alice@test.com | alice123 | USER |
| bob@test.com | bob12345 | USER |
| carol@test.com | carol123 | USER |

## Переменные окружения

| Переменная | По умолчанию |
|---|---|
| `PORT` | 8080 |
| `JWT_SECRET` | dev-значение, задать своё на VPS (мин. 32 символа) |
| `JWT_TTL_MINUTES` | 60 |
