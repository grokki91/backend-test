# Postman-коллекция для ручного тестирования

1. Подними стек: `docker compose up -d --build`.
2. Импортируй `backend-test.postman_collection.json` в Postman (File → Import).
   Отдельное окружение не нужно — `baseUrl` и остальные переменные лежат
   на уровне коллекции и заполняются автоматически test-скриптами по мере
   прохождения запросов.
3. Проходи папки по порядку сверху вниз:
   - `0. Health & Actuator` — жив ли инстанс.
   - `1. Auth` — логины alice/bob/admin и OAuth client_credentials кладут
     токены в переменные `tokenAlice`/`tokenBob`/`tokenAdmin`/`tokenClient`.
   - `2. Users v1` / `3. Users v2` — CRUD, права доступа (owner/ADMIN), 403-кейсы.
   - `4. Orders` — создание с `Idempotency-Key`, replay, ETag/`If-Match`,
     переходы статусов, недопустимые переходы (409), `poison` для DLQ/DLT.
   - `5. Webhooks` — подписка на https://webhook.site (подставь свой URL).
   - `6. Shipping & Chaos` — WireMock-квота и chaos-эндпоинты.
   - `7. GraphQL` — те же данные через `/graphql`.
   - `8. SOAP` — WSDL и `GetOrderRequest`.
   - `9. WebSocket & SSE` — SSE можно дёрнуть прямо из Postman, WebSocket
     удобнее вручную (`wscat` или Postman → New → WebSocket Request).
   - `10. Rate limit` — прогони один запрос через Collection Runner
     (110 итераций, delay 0) и проверь 429 после 100-го.

Тестовые пользователи и роли — в основном README репозитория.
