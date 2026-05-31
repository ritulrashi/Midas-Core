# Midas Core

Distributed transaction processing system built with Java 17, Spring Boot 3, Apache Kafka, and PostgreSQL. Implements an event-driven architecture where a central transaction service publishes domain events consumed independently by reconciliation, ledger, and notification workers.

---

## Architecture

```
                         ┌─────────────────────────┐
                         │          Clients         │
                         └────────────┬────────────┘
                                      │ HTTPS
                                      ▼
                         ┌─────────────────────────┐
                         │   api-gateway  (:8080)  │
                         │  JWT validation          │
                         │  Route → downstream      │
                         │  Injects X-User-Id /     │
                         │  X-User-Role headers     │
                         └──────┬──────────┬────────┘
                                │          │
                                ▼          ▼
                  ┌──────────────┐  ┌────────────────────┐
                  │ auth-service │  │ transaction-service │
                  │   (:8081)    │  │      (:8082)        │
                  │ Registration │  │  Create / query     │
                  │ Login / JWT  │  │  transactions       │
                  │ midas_auth   │  │  midas_transactions │
                  └──────────────┘  └──────────┬──────────┘
                                               │ Kafka publish
                                               ▼
                              ┌────────────────────────────┐
                              │   midas.transaction.events │
                              │       (3 partitions)       │
                              └────┬──────────┬────────────┘
                                   │          │          │
                    ┌──────────────┘          │          └──────────────┐
                    ▼                         ▼                         ▼
       ┌────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
       │ reconciliation-svc │  │  notification-worker │  │    ledger-worker    │
       │      (:8083)       │  │       (:8084)        │  │       (:8085)       │
       │ Scheduled recon    │  │  Email / push        │  │ Double-entry        │
       │ midas_reconcil..   │  │  dispatch            │  │ bookkeeping         │
       └────────────────────┘  └─────────────────────┘  │ midas_ledger        │
                                                         └─────────────────────┘
```

**JWT flow:** `auth-service` issues HS256 tokens. `api-gateway` validates the token on every request, strips it, and injects `X-User-Id` and `X-User-Role` headers. Downstream services trust those headers and never re-validate the JWT.

**Kafka fan-out:** Each consumer service uses a distinct `group.id`, so all three receive every event independently. Consumer failures are not acked — Kafka redelivers automatically. Unrecoverable events are forwarded to `midas.transaction.events.dlq`.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Gateway | Spring Cloud Gateway 2023.0.1 |
| Messaging | Apache Kafka 7.6.0 (Confluent) |
| Persistence | PostgreSQL 16, Spring Data JPA, Hibernate |
| Migrations | Flyway |
| Auth | JJWT 0.12.5 (HS256) |
| Boilerplate | Lombok |
| Build | Maven (multi-module) |
| Containerisation | Docker Compose |

---

## Modules

| Module | Port | Database | Responsibility |
|---|---|---|---|
| `common` | — | — | Shared DTOs, `TransactionEvent`, `EventType` |
| `api-gateway` | 8080 | — | JWT validation, routing, CORS |
| `auth-service` | 8081 | `midas_auth` | Registration, login, JWT issuance |
| `transaction-service` | 8082 | `midas_transactions` | Create/query/update transactions, Kafka producer |
| `reconciliation-service` | 8083 | `midas_reconciliation` | Kafka consumer, scheduled reconciliation (every 30 min) |
| `notification-worker` | 8084 | — | Kafka consumer, email/push dispatch |
| `ledger-worker` | 8085 | `midas_ledger` | Kafka consumer, double-entry bookkeeping |

---

## Infrastructure

| Service | Host port | Image |
|---|---|---|
| PostgreSQL | 5432 | `postgres:16-alpine` |
| Kafka | 9092 | `confluentinc/cp-kafka:7.6.0` |
| Zookeeper | 2181 | `confluentinc/cp-zookeeper:7.6.0` |
| Kafka UI | 8090 | `provectuslabs/kafka-ui` |

### Kafka topics

| Topic | Partitions | Purpose |
|---|---|---|
| `midas.transaction.events` | 3 | Primary domain event bus |
| `midas.transaction.events.dlq` | 1 | Dead-letter queue for failed consumer events |

---

## Running Locally

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.9+ (or use the included `./mvnw` wrapper)

### 1. Configure environment

```bash
cp .env.example .env
```

Open `.env` and set a real `JWT_SECRET` (minimum 32 characters):

```dotenv
JWT_SECRET=your-256-bit-secret-here-min-32-chars
```

All other defaults work for local development.

### 2. Start infrastructure

```bash
docker compose up -d
```

Wait for PostgreSQL and Kafka to be healthy before starting services. Kafka UI is available at http://localhost:8090.

### 3. Build all modules

```bash
./mvnw clean install -DskipTests
```

### 4. Start services

Each service is a standard Spring Boot application. Open a terminal per service:

```bash
# Terminal 1 — gateway (start first, depends on nothing)
cd api-gateway && ../mvnw spring-boot:run

# Terminal 2 — auth
cd auth-service && ../mvnw spring-boot:run

# Terminal 3 — transactions
cd transaction-service && ../mvnw spring-boot:run

# Terminal 4 — reconciliation
cd reconciliation-service && ../mvnw spring-boot:run

# Terminal 5 — notifications
cd notification-worker && ../mvnw spring-boot:run

# Terminal 6 — ledger
cd ledger-worker && ../mvnw spring-boot:run
```

Flyway runs automatically on startup and creates all required schema objects.

### Environment variables reference

| Variable | Default | Description |
|---|---|---|
| `POSTGRES_USER` | `midas` | PostgreSQL username |
| `POSTGRES_PASSWORD` | `midas_secret` | PostgreSQL password |
| `JWT_SECRET` | *(none)* | HS256 signing key — **required in production** |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL in milliseconds (default: 24 h) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `API_GATEWAY_PORT` | `8080` | |
| `AUTH_SERVICE_PORT` | `8081` | |
| `TRANSACTION_SERVICE_PORT` | `8082` | |
| `RECONCILIATION_SERVICE_PORT` | `8083` | |
| `NOTIFICATION_WORKER_PORT` | `8084` | |
| `LEDGER_WORKER_PORT` | `8085` | |

---

## API Reference

All requests go through the gateway at `http://localhost:8080`. Endpoints under `/api/transactions` and `/api/reconciliation` require a valid JWT in the `Authorization: Bearer <token>` header.

### Auth — `/api/auth`

#### `POST /api/auth/register`

Create a new user account and receive a JWT.

**Request body:**
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "securepassword"
}
```

| Field | Type | Constraints |
|---|---|---|
| `username` | string | 3–50 characters |
| `email` | string | valid email |
| `password` | string | 8–100 characters |

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "User registered",
  "data": {
    "token": "<jwt>",
    "type": "Bearer"
  }
}
```

---

#### `POST /api/auth/login`

Authenticate and receive a JWT.

**Request body:**
```json
{
  "email": "alice@example.com",
  "password": "securepassword"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "token": "<jwt>",
    "type": "Bearer"
  }
}
```

---

### Transactions — `/api/transactions`

All endpoints require `Authorization: Bearer <token>`.

#### `POST /api/transactions`

Create a new transaction. Persists with status `PENDING` and publishes a `TRANSACTION_CREATED` event to Kafka.

**Request body:**
```json
{
  "type": "TRANSFER",
  "amount": "250.00",
  "currency": "USD",
  "fromAccount": "acc-001",
  "toAccount": "acc-002",
  "description": "Invoice payment"
}
```

| Field | Type | Constraints |
|---|---|---|
| `type` | `TransactionType` enum | `TRANSFER`, `PAYMENT`, `REFUND`, etc. |
| `amount` | decimal | ≥ 0.01, max 15 integer digits / 4 decimal places |
| `currency` | string | exactly 3 characters (ISO 4217) |
| `fromAccount` | string | required |
| `toAccount` | string | required |
| `description` | string | optional, max 500 characters |

**Response `201 Created`:**
```json
{
  "success": true,
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "type": "TRANSFER",
    "amount": 250.00,
    "currency": "USD",
    "fromAccount": "acc-001",
    "toAccount": "acc-002",
    "status": "PENDING",
    "initiatedBy": "<user-id>",
    "description": "Invoice payment",
    "createdAt": "2026-05-30T10:00:00Z",
    "updatedAt": "2026-05-30T10:00:00Z"
  }
}
```

---

#### `GET /api/transactions/{id}`

Fetch a single transaction by UUID.

**Response `200 OK`:** Transaction object (same shape as create response).

---

#### `PATCH /api/transactions/{id}/status`

Update the status of a transaction and publish the corresponding Kafka event.

**Request body:**
```json
{
  "status": "COMPLETED",
  "failureReason": null
}
```

| Field | Type | Constraints |
|---|---|---|
| `status` | `TransactionStatus` enum | `PROCESSING`, `COMPLETED`, `FAILED`, `REVERSED` |
| `failureReason` | string | optional; use when status is `FAILED` |

**Kafka events emitted per status:**

| Status | Event published |
|---|---|
| `PROCESSING` | `TRANSACTION_PROCESSING` |
| `COMPLETED` | `TRANSACTION_COMPLETED` |
| `FAILED` | `TRANSACTION_FAILED` |
| `REVERSED` | `TRANSACTION_REVERSED` |

**Response `200 OK`:** Updated transaction object.

---

#### `GET /api/transactions/my`

Paginated list of transactions initiated by the authenticated user.

**Query parameters:** standard Spring `Pageable` (`page`, `size`, `sort`). Default: 20 per page, sorted by `createdAt`.

**Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "content": [ /* transaction objects */ ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0,
    "size": 20
  }
}
```

---

## Event Schema

All Kafka messages are JSON-serialised `TransactionEvent` objects:

```json
{
  "eventId": "uuid",
  "transactionId": "uuid",
  "eventType": "TRANSACTION_CREATED",
  "fromAccount": "acc-001",
  "toAccount": "acc-002",
  "amount": 250.00,
  "currency": "USD",
  "status": "PENDING",
  "initiatedBy": "user-id",
  "failureReason": null,
  "occurredAt": "2026-05-30T10:00:00Z"
}
```

Events are keyed by `transactionId`, guaranteeing ordered delivery per transaction across all partitions.

---

## Design Notes

**Idempotent consumers** — each consumer checks `transactionId` before processing to handle Kafka redeliveries safely.

**Schema evolution** — `TransactionEvent` is annotated `@JsonIgnoreProperties(ignoreUnknown = true)` so older consumer deployments survive new fields being added.

**Dead-letter queue** — consumer exceptions re-publish the original payload plus error context to `midas.transaction.events.dlq` for manual inspection and replay.

**Outbox pattern (TODO)** — `transaction-service` should write the Kafka event inside the same DB transaction via an outbox table + relay job to guarantee exactly-once publishing and eliminate the current dual-write window.

**Rate limiting (TODO)** — add Redis + Spring Cloud Gateway `RequestRateLimiter` filter per route.

**RS256 rotation (TODO before prod)** — `auth-service` currently issues HS256 tokens with a shared secret. Rotate to RS256 and distribute the public key to all services so the gateway can validate without sharing a secret.
