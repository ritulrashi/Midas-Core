# Midas Core

Distributed transaction processing system — Java 17, Spring Boot 3, Apache Kafka, PostgreSQL.

## Architecture

```
          ┌──────────────────────────┐
          │         Clients          │
          └────────────┬─────────────┘
                       │ HTTPS
                       ▼
          ┌──────────────────────────┐
          │   api-gateway  (:8080)   │
          │  JWT auth + routing      │
          └────┬──────────┬──────────┘
               │          │
               ▼          ▼
   ┌──────────────┐  ┌───────────────────┐
   │ auth-service │  │ transaction-svc   │
   │   (:8081)    │  │    (:8082)        │
   │  PostgreSQL  │  │  PostgreSQL       │
   └──────────────┘  └────────┬──────────┘
                              │ Kafka publish
                              ▼
                 ┌────────────────────────┐
                 │  midas.transaction     │
                 │  .events  (3 parts)    │
                 └──┬──────────┬────────┬─┘
                    │          │        │
                    ▼          ▼        ▼
           ┌──────────┐ ┌──────────┐ ┌──────────┐
           │reconcili-│ │notif-    │ │ledger-   │
           │ation-svc │ │worker    │ │worker    │
           │  (:8083) │ │ (:8084)  │ │ (:8085)  │
           │PostgreSQL│ │          │ │PostgreSQL│
           └──────────┘ └──────────┘ └──────────┘
```

## Modules

| Module | Port | Database | Role |
|---|---|---|---|
| `common` | — | — | Shared library: events, DTOs |
| `api-gateway` | 8080 | — | Spring Cloud Gateway — JWT validation, routing |
| `auth-service` | 8081 | `midas_auth` | User registration, login, JWT issuance |
| `transaction-service` | 8082 | `midas_transactions` | Create/query transactions, Kafka producer |
| `reconciliation-service` | 8083 | `midas_reconciliation` | Kafka consumer, scheduled reconciliation |
| `notification-worker` | 8084 | `midas_notifications` | Kafka consumer, email/push dispatch |
| `ledger-worker` | 8085 | `midas_ledger` | Kafka consumer, double-entry bookkeeping |

## Infrastructure (Docker Compose)

| Service | Port | Image |
|---|---|---|
| Kafka | 9092 (host), 29092 (internal) | confluentinc/cp-kafka:7.6.0 |
| Zookeeper | 2181 | confluentinc/cp-zookeeper:7.6.0 |
| PostgreSQL | 5432 | postgres:16-alpine |
| Kafka UI | 8090 | provectuslabs/kafka-ui |

## Kafka Topics

| Topic | Partitions | Consumers |
|---|---|---|
| `midas.transaction.events` | 3 | reconciliation-service, notification-worker, ledger-worker |
| `midas.transaction.events.dlq` | 1 | manual replay / alerting |

Each consumer service uses a distinct `group.id` so all three receive every event independently.

## JWT Strategy

- `auth-service` issues HS256 tokens signed with `JWT_SECRET`.
- `api-gateway` validates the token, then injects `X-User-Id` and `X-User-Role` headers.
- Downstream services trust those headers — they never re-validate the JWT.
- **TODO before prod**: Rotate to RS256; distribute the public key to all services.

## Development Setup

```bash
# 1. Start infrastructure
cp .env.example .env          # fill in JWT_SECRET
docker compose up -d

# 2. Build everything
./mvnw clean install -DskipTests

# 3. Run a service
cd auth-service && ../mvnw spring-boot:run
```

## Key Design Decisions

- **Outbox pattern (TODO)**: `transaction-service` should write the Kafka event inside the same DB transaction using an outbox table + relay job to guarantee exactly-once publishing.
- **DLQ**: Consumer errors are caught and re-published to `midas.transaction.events.dlq` with the original payload and error context for manual inspection.
- **Idempotent consumers**: Each consumer checks `transactionId` before processing to handle Kafka redeliveries safely.
- **Schema evolution**: Use a shared `TransactionEvent` DTO in the `common` module. Add fields with `@JsonIgnoreProperties(ignoreUnknown = true)` on consumers so older deployments don't break.
- **Rate limiting (TODO)**: Add Redis + Spring Cloud Gateway `RequestRateLimiter` filter per route.
