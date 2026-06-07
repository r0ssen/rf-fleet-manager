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

**Stack:** Spring MVC + Thymeleaf · Spring Data JPA (Hibernate) · Flyway migrations · PostgreSQL (prod) / H2 (dev/test) · Spring Security (form login, role-based)

**Domain:** Festival fleet management for RFG. Core entities:
- `Vehicle` — cargo capacity, equipment, category
- `Task` — transportation booking with status lifecycle: `ORDERED → STARTED → DONE | CANCELLED`
- `OpeningHours` — fleet availability windows by location/date
- `VehicleAvailability` — per-vehicle availability by day
- `User` — system users with roles ADMIN or AGENT

**Key pattern — multi-tenancy by festival year:** All entities embed `festivalYear` as part of their composite keys. Queries are always scoped to a configured festival year (set per Spring profile).

**Layered structure:**
- `controller/` — Spring MVC controllers (fleet grid, tasks, vehicles, opening hours, user admin)
- `service/` — business logic (`TaskService`, `VehicleService`, `FleetGridService`)
- `repository/` — Spring Data JPA interfaces
- `entity/` — JPA domain model with `@Version` optimistic locking on mutable entities
- `config/` — `SecurityConfig`, `AppConfig`, `DateTimeBindingConfig`, `GlobalExceptionHandler`, `UserDetailsServiceImpl`, `DataInitializer`
- `util/` — `DivisionTeamData` helper

**Database migrations:** Two parallel sets — `db/migration/` (PostgreSQL, used in staging/prod) and `db/migration-h2/` (H2 dialect, used in dev/test). When adding a migration, add it to **both** directories. Current highest version: `V5__users.sql`.

**Driver suggestion logic:** `TaskService` finds the most recent driver for a given vehicle on the same day — this is covered by `TaskRepositoryIntegrationTest` as a regression test and should not be broken.

## Security

Spring Security with form login. Two roles:
- `ADMIN` — full access to everything
- `AGENT` — read-only access to vehicles; full access to tasks and fleet view

Key rules in `SecurityConfig`:
- `GET /vehicles`, `GET /vehicles/*/edit` — authenticated (any role)
- `/vehicles/**`, `/opening-hours/**`, `/admin/**` — ADMIN only
- All other routes — authenticated

Default seeded users (via `DataInitializer`): `admin`/`admin` (ADMIN) and `agent`/`agent` (AGENT). Seeds only if the `users` table is empty.

`DataInitializer.run()` is wrapped in try-catch to handle integration tests that don't create the `users` table.

## User Administration

`/admin/users` — CRUD for system users (ADMIN only). Prevents deleting the last admin account.

## Date Pickers

All date inputs use **flatpickr** (loaded via CDN in `fragments/layout.html`) with Danish locale and `dd/mm/yyyy` display format. The underlying hidden inputs and server communication always use ISO `yyyy-MM-dd`. Flatpickr is initialized per-page in a `DOMContentLoaded` block at the bottom of each template that has a date picker.

Affected templates:
- `fleet/grid.html` — `#fleetDatePicker`
- `tasks/list.html` — `#taskDatePicker`
- `tasks/form.html` — `#startDateDisplay` (with hidden `#startDate` for ISO)
- `opening-hours/list.html` — `#festivalDateDisplay` (with hidden `#festivalDate` for ISO)

## Task Features

- **Dispatcher field removed** — `receivedBy` is auto-set to the logged-in user on creation; preserved on edit; shown read-only on the edit form.
- **DONE tasks hidden by default** in the task list — "Vis udførte" checkbox toggles them.
- **Overdue badge** — ORDERED tasks whose start time has passed show a `!` badge in the task list.

## Profiles

| Profile | Database | DDL | Notes |
|---|---|---|---|
| `dev` | H2 in-memory | create-drop | H2 console enabled |
| `staging` | PostgreSQL (env vars) | validate | Flyway baseline enabled |
| (default) | PostgreSQL | validate | Railway deployment |

## Deployment

CI/CD via `.github/workflows/pipeline.yml`: builds, runs regression test, runs all tests, deploys to Railway on push to `main`.
