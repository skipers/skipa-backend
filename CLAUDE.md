# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Local Development

```bash
# First time (or after deleting local DB file)
./gradlew h2CreateLocalDb

# Terminal 1: start H2 TCP server
./gradlew h2Server

# Terminal 2: start application
./gradlew bootRun
```

The app reads `.env` from the project root automatically — no need to set env vars separately in the IDE.

**Required `.env` entries:**
```properties
JWT_SECRET=<base64-encoded 32+ byte key>
SPRING_PROFILES_ACTIVE=local
LOCAL_SEED_PASSWORD=1234
```

### Build & Test

```bash
./gradlew build          # full build
./gradlew test           # all tests
./gradlew test --tests "com.skipers.skipa.domain.auth.*"  # single package
```

### Dev Tools

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 Console: `http://localhost:8082` (JDBC URL: `jdbc:h2:tcp://localhost/~/skipa`, user: `sa`, password: empty)

## Architecture

### Tech Stack

Spring Boot 4.0.6 · Java 17 · Gradle · Spring Security + JWT (jjwt 0.13) · Spring Data JPA · Flyway · H2 (local) / PostgreSQL (prod) · springdoc-openapi 3.0

### Package Layout

```
com.skipers.skipa
├── global/           # Cross-cutting infrastructure
│   ├── config/       # Security, JPA auditing, Jackson, OpenAPI, local seed
│   ├── exception/    # ErrorCode enum, BusinessException, GlobalExceptionHandler
│   ├── response/     # ApiResponse<T> (success), ErrorResponse (failure), PageResponse
│   └── security/     # JwtProvider, JwtAuthenticationFilter, CustomUserDetails*
└── domain/           # Business logic, one package per domain
    ├── auth/         # Login, token refresh, /me
    ├── user/         # Admin user management & approval
    ├── department/   # Department CRUD + deactivation
    ├── patent/       # Patent CRUD, legal status, annuities, business review screen
    ├── review/       # Review cycles, review requests, Legal monitoring
    └── report/       # AI evaluation report generation and query
```

Each domain follows a strict internal structure:
- `api/` — Controllers (HTTP mapping, validation)
- `application/` — Services (use cases, transaction boundaries)
- `dao/` — Spring Data JPA repositories
- `domain/` — Entities and enums
- `dto/` — Request/response records with validation + Swagger annotations
- `exception/` — Domain-specific exceptions extending `BusinessException`

Infrastructure connectors (AI server, S3, KIPRIS) live under `infra/` (partially implemented).

### Response Shape

All controllers return `ApiResponse<T>` (success) or `ErrorResponse` (failure) — these are **different top-level structures**. Success: `{ success: true, data: ... }`. Failure: `{ success: false, error: { code, message } }`.

Throw `BusinessException(ErrorCode.*)` for domain errors — `GlobalExceptionHandler` converts them automatically. Every error code must be added to `ErrorCode` enum first.

### Security

- JWT access token (10 min) + refresh token (7 days) issued on login.
- `JwtAuthenticationFilter` reads `Authorization: Bearer <token>` and populates `SecurityContext`.
- `UserRole` values: `ADMIN`, `LEGAL`, `BUSINESS`. Access control enforced in `SecurityConfig` and service layer.
- `BUSINESS` users can only access patents where the assigned department matches their own department.
- `LEGAL` users perform all write operations (legal status, annuities, review requests, report generation).
- `ADMIN` users have read-only access to everything; approval of new users is admin-only.

### Database Profiles

| Profile | DB | `ddl-auto` |
|---|---|---|
| `local` | H2 TCP file (`~/skipa`) | `update` |
| `prod` | PostgreSQL | `validate` |

Prod schema changes go through Flyway migrations in `src/main/resources/db/migration/`. Local uses H2 with `ddl-auto: update` so no migrations needed for local dev.

On local startup, if the `users` table is empty, `LocalDataInitializer` seeds 1 admin, 4 legal, 5 business accounts with password from `LOCAL_SEED_PASSWORD`.

### Key Business Policies

- **Department deactivation**: departments are never deleted; they become `INACTIVE`. Inactive departments cannot be used for new user approvals, patent assignments, or review requests.
- **Enum values**: all enums stored and returned as uppercase English strings (e.g. `REGISTERED`, `PAID`, `GENERATING`, `PENDING`, `MAINTAIN`).
- **`/assigned-patents`**: business-unit review screen — returns the most recent review request per patent+department pair.
