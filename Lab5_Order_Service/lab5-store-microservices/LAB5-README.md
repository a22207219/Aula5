# Lab 5 — The Order Service

Project contains three Maven modules: `product-service` (8081), `supplier-service` (8082), and `order-service` (8083), each with its own database in a shared PostgreSQL 18 container. The Order Service integrates with the two other services over HTTP. Flyway migrations live under each module's `src/main/resources/db/migration`.

## Prerequisites and launch

Java 25, Docker Desktop with Docker Compose v2. From the project root:

```powershell
Copy-Item .env.example .env
# Edit .env to set your own passwords.
docker network inspect store-network *> $null
if ($LASTEXITCODE -ne 0) { docker network create store-network }
.\mvnw.cmd clean verify
docker compose config
docker compose up --build -d
docker compose ps
```

The network-inspection command creates `store-network` only when it does not already exist. For Linux/macOS use `cp .env.example .env`, `docker network inspect store-network >/dev/null 2>&1 || docker network create store-network`, and `./mvnw clean verify`. Do not include `.env` in submissions. To inspect failures: `docker compose logs --tail=200 order-service product-service supplier-service store-db`.

## Orders API

* `POST /api/v1/orders` with JSON `{"productId":"<UUID>","quantity":2}`
* `GET /api/v1/orders` and `GET /api/v1/orders/{id}`
* `PUT /api/v1/orders/{id}` and `DELETE /api/v1/orders/{id}`
* `POST /api/v1/orders/{id}/confirm` and `POST /api/v1/orders/{id}/cancel`

Product stock endpoints: `GET /api/v1/products/{id}/stock`, `POST /api/v1/products/{id}/stock/decrement`, and `/increment`. Both write endpoints require `{"quantity":2,"operationId":"<UUID>"}`. Create an active supplier and product via the existing Lab 4 APIs, then seed stock with an increment using a unique operation ID.

## Consistency, idempotency and recovery

Creating an order does not change stock. Order persists intermediate `CONFIRMING` or `CANCELLING` transitions before calling Product. Product persists a stock-operation ledger in the same transaction as the stock change, and repeated calls carry a deterministic operation ID; duplicate calls do not reapply stock. An unsuccessful remote operation does not mark the order successful. Insufficient stock returns the confirmation to `PENDING`. Cancellation restores stock when required. A scheduled `OrderRecovery` scans interrupted transitions and replays them with the same operation ID (15-second polling, 30-second minimum age by default). Recovery depends on the remote service becoming available; this is eventual reconciliation, not distributed ACID.

## Checks

Run `./mvnw clean verify` (Windows ` .\mvnw.cmd clean verify`), `docker compose config`, `docker compose up --build -d`, then `docker compose ps`. The provided scripts are `scripts/static-check.ps1`, `scripts/validate-compose.ps1`, `scripts/smoke-test.ps1` (Lab 4 cross-service checks), `scripts/lab5-smoke-test.ps1` (end-to-end order/stock flow), and `scripts/resilience-test.ps1`. Run them from PowerShell as `./scripts/<script-name>.ps1` as appropriate. Check repeated confirm/cancel requests and restart Product during an in-flight action to verify eventual recovery.

**Validation status:** the archive's integrity and structure were inspected here. Java 25, Docker and running PostgreSQL were not available in the packaging environment, so compilation and end-to-end tests must be run in the target environment. Do not report them as passed without actually running them.
