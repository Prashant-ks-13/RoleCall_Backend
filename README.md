# RoleCall Backend

Microservices backend for the RoleCall job board (companion to [RoleCall_frontend](https://github.com/Prashant-ks-13/RoleCall_frontend)).

## Architecture

Spring Boot microservices, service discovery via Eureka, centralized config via Spring Cloud Config, single entry point via Spring Cloud Gateway, async inter-service communication via Kafka, one MySQL schema per service.

| Service | Responsibility | Port |
|---|---|---|
| `eureka-server` | Service discovery | 8761 |
| `config-server` | Centralized non-secret config | 8888 |
| `api-gateway` | Single entry point, routing, JWT fast-fail | 8080 |
| `auth-service` | Registration, login, JWT issuance/refresh, JWKS | 8081 |
| `user-service` | User profiles | 8082 |
| `job-service` | Job postings, search | 8083 |
| `application-service` | Job applications, status tracking | 8084 |
| `payment-service` | Stripe (test mode) payments for featured listings | 8085 |

See [docs/kafka-topics.md](docs/kafka-topics.md) for the event contract between services and [docs/smoke-test.http](docs/smoke-test.http) for an end-to-end manual verification script.

## Local development

Prerequisites: Docker, Docker Compose. (A local JDK/Maven install is optional — services build inside Docker via multi-stage builds.)

```bash
cp .env.example .env   # fill in real secrets
docker compose up --build
```

Services register with Eureka at `http://localhost:8761` and are reachable through the gateway at `http://localhost:8080`.

## Repository layout

Each service is an independently buildable Maven module with its own `pom.xml`, Flyway migrations, `Dockerfile`, and Spring Security config. `rolecall-common` is a thin shared module containing only Kafka event POJOs, the common API error shape, and role-name constants — no shared entities, repositories, or Security auto-configuration, so services stay independently deployable.

Each service was developed on its own branch (`feature/<service-name>`) and merged into `main`.
