# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```powershell
# Run locally with H2 in-memory database (no PostgreSQL needed)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run all tests
mvn test

# Run a single test class
mvn -Dtest=TaskServiceTest test

# Build the project
mvn clean package

# Set up JDK 21 and Maven on Windows (run once per shell session)
.\scripts\use-jdk21.ps1
```

Dev profile: H2 console at `/h2-console`, Spring Security defaults `admin`/`admin`, festival year 2026.

## Architecture

Spring Boot 4.0.6 (Java 21) full-stack app with Thymeleaf server-side rendering. No frontend build tools — all HTML is rendered server-side with no npm/webpack pipeline.

**Stack:** Spring MVC + Thymeleaf · Spring Data JPA (Hibernate) · Flyway migrations · PostgreSQL (prod) / H2 (dev/test) · Spring Security (currently all routes public)

**Domain:** Festival fleet management for RFG. Core entities:
- `Vehicle` — cargo capacity, equipment, category
- `Task` — transportation booking with status lifecycle: `ORDERED → STARTED → DONE | CANCELLED`
- `OpeningHours` — fleet availability windows by location/date
- `VehicleAvailability` — per-vehicle availability by day

**Key pattern — multi-tenancy by festival year:** All entities embed `festivalYear` as part of their composite keys. Queries are always scoped to a configured festival year (set per Spring profile).

**Layered structure:**
- `controller/` — Spring MVC controllers (fleet grid, tasks, vehicles, opening hours)
- `service/` — business logic (`TaskService`, `VehicleService`, `FleetGridService`)
- `repository/` — Spring Data JPA interfaces
- `entity/` — JPA domain model with `@Version` optimistic locking on mutable entities
- `config/` — `SecurityConfig`, `AppConfig`, `DateTimeBindingConfig`, `GlobalExceptionHandler`
- `util/` — `DivisionTeamData` helper

**Database migrations:** Two parallel sets — `db/migration/` (PostgreSQL, used in staging/prod) and `db/migration-h2/` (H2 dialect, used in dev/test). When adding a migration, add it to both directories.

**Driver suggestion logic:** `TaskService` finds the most recent driver for a given vehicle on the same day — this is covered by `TaskRepositoryIntegrationTest` as a regression test and should not be broken.

## Profiles

| Profile | Database | DDL | Notes |
|---|---|---|---|
| `dev` | H2 in-memory | create-drop | H2 console enabled |
| `staging` | PostgreSQL (env vars) | validate | Flyway baseline enabled |
| (default) | PostgreSQL | validate | Railway deployment |

## Deployment

CI/CD via `.github/workflows/pipeline.yml`: builds, runs regression test, runs all tests, deploys to Railway on push to `main`.
