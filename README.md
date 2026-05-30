# Stepcore Security — Backend API

REST API for the **Stepcore Security** payroll time-tracking and earnings calculation system.  
Built with **Java 17 · Spring Boot 3.3 · PostgreSQL 15 · Flyway · JWT**.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Local Environment Setup](#local-environment-setup)
- [Running the Application](#running-the-application)
- [API Reference](#api-reference)
- [Default Credentials](#default-credentials)
- [Running Tests](#running-tests)
- [Environment Variables](#environment-variables)
- [Security Model](#security-model)

---

## Overview

Stepcore Security manages employee time records and calculates earnings based on configurable payroll rules.  
This backend exposes a versioned REST API (`/api/v1/…`) consumed by the React web frontend and the mobile application.

Current modules implemented in this repository:

| Module | Description |
|--------|-------------|
| **Security** | JWT authentication, role-based access control, user management, audit log |

Upcoming modules (planned):

| Module | Description |
|--------|-------------|
| Employee Config | Employee profiles, hourly rates, contracts |
| Time Tracking | Clock-in / clock-out, INCOMPLETE record handling |
| Payroll Config | Surcharge types, overtime rules, holiday calendar |
| Reports | Time summaries, earnings calculations, Excel export |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│              React Frontend (Port 3000)              │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP / JSON  (JWT Bearer)
┌──────────────────────▼──────────────────────────────┐
│         Spring Boot 3 REST API  (Port 8080)          │
│                                                      │
│  AuthController   RoleController   UserController    │
│       │                │                │            │
│  AuthService      RoleService      UserService       │
│       │                │                │            │
│  UserRepository  RoleRepository  AuditLogRepository  │
└──────────────────────┬──────────────────────────────┘
                       │ JDBC / JPA
┌──────────────────────▼──────────────────────────────┐
│          PostgreSQL 15  (Port 5432)                  │
│  roles · menu_options · role_menu_options            │
│  users · audit_logs                                  │
└─────────────────────────────────────────────────────┘
```

**Security flow:**  
`POST /api/v1/auth/login` → validate credentials → issue JWT (HMAC-SHA256, 24 h) → client sends `Authorization: Bearer <token>` on every subsequent request → `JwtAuthenticationFilter` validates token → Spring Security context populated → `@PreAuthorize` enforces RBAC.

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.5 |
| Security | Spring Security + JJWT | 6.x + 0.12.6 |
| Persistence | Spring Data JPA + Hibernate | 6.x |
| Database | PostgreSQL | 15 |
| Migrations | Flyway | 10.x |
| Mapping | MapStruct | 1.5.5.Final |
| Code gen | Lombok | 1.18.x |
| API docs | SpringDoc OpenAPI (Swagger UI) | 2.6.0 |
| Build | Maven | 3.6+ |
| Containerization | Docker / Docker Compose | 20+ |
| Testing | JUnit 5 + Mockito + AssertJ + Testcontainers | — |
| Coverage | JaCoCo (≥ 90% threshold) | 0.8.12 |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/stepcore/security/
│   │   ├── StepcoreSecurityApplication.java
│   │   ├── common/           # ApiResponse<T> record
│   │   ├── config/           # SecurityConfig, OpenApiConfig, JwtProperties, DataSeeder
│   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── auth/     # LoginRequest, LoginResponse, ChangePasswordRequest
│   │   │   │   ├── role/     # CreateRoleRequest, UpdateRoleRequest, RoleResponse, MenuOptionResponse
│   │   │   │   └── user/     # CreateUserRequest, UpdateUserRequest, UserResponse, UserStatusRequest
│   │   │   ├── mapper/       # RoleMapper, UserMapper  (MapStruct)
│   │   │   ├── AuthController.java
│   │   │   ├── RoleController.java
│   │   │   └── UserController.java
│   │   ├── domain/model/     # User, Role, MenuOption, AuditLog  (JPA entities)
│   │   ├── exception/        # GlobalExceptionHandler + domain exceptions
│   │   ├── repository/       # Spring Data JPA interfaces
│   │   ├── security/         # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
│   │   └── service/          # AuthService, RoleService, UserService, AuditService
│   └── resources/
│       ├── application.yml
│       └── db/migration/     # Flyway SQL scripts V1–V4
└── test/
    ├── java/com/stepcore/security/
    │   ├── controller/       # AuthControllerIT, RoleControllerIT, UserControllerIT
    │   └── service/          # AuthServiceTest, RoleServiceTest, UserServiceTest, JwtServiceTest
    └── resources/
        └── application.yml   # Testcontainers datasource config
```

---

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| Java JDK | 17 | `java -version` |
| Maven | 3.6.3 | `mvn -version` |
| Docker Desktop | 20.x | Required for PostgreSQL container |
| Git | Any | `git --version` |

---

## Local Environment Setup

### 1. Clone the repository

```bash
git clone https://github.com/jfranciscogomezn/stepcore-security-backend.git
cd stepcore-security-backend
```

### 2. Configure environment variables (optional)

Copy the example file and adjust values if needed:

```bash
cp ../.env.example .env
```

Default values work out of the box for local development — no changes needed.

### 3. Start the database

From the **repository root** (where `docker-compose.yml` lives):

```bash
docker compose up -d postgres
```

Wait until the container is healthy:

```bash
docker compose ps
# Expected: stepcore_security_db   Up (healthy)
```

Flyway migrations run automatically on first application startup and create all tables + seed data.

### 4. Build the application

```bash
mvn package -DskipTests
```

---

## Running the Application

### Option A — Run the JAR directly

```bash
java -jar target/stepcore-security-backend-1.0.0-SNAPSHOT.jar
```

### Option B — Run via Maven

```bash
mvn spring-boot:run
```

### Option C — Run inside Docker (full stack)

> Coming soon — multi-stage `Dockerfile` is already in the repository.

---

### Verify the application started

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot :: (v3.3.5)
 ...
 INFO  --- Started StepcoreSecurityApplication in X.XXX seconds
```

The API is ready at **`http://localhost:8080`**.  
Swagger UI is available at **`http://localhost:8080/swagger-ui.html`**.

---

## API Reference

### Base URL

```
http://localhost:8080/api/v1
```

### Authentication

All endpoints except `POST /auth/login` require an `Authorization: Bearer <token>` header.

### Endpoints

#### Auth

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| `POST` | `/auth/login` | Public | Log in, receive JWT |
| `POST` | `/auth/logout` | Authenticated | Invalidate session (client-side) |
| `GET` | `/auth/me` | Authenticated | Current user profile + menu options |
| `PATCH` | `/auth/change-password` | Authenticated | Change own password |

#### Roles (ADMIN only)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/roles` | List all roles |
| `POST` | `/roles` | Create role |
| `GET` | `/roles/{id}` | Get role by ID |
| `PUT` | `/roles/{id}` | Update role |
| `DELETE` | `/roles/{id}` | Delete role (fails if users assigned) |
| `GET` | `/roles/{id}/menu-options` | Get assigned menu options |
| `PUT` | `/roles/{id}/menu-options` | Replace menu option assignment |

#### Users (ADMIN only)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/users` | List all users |
| `POST` | `/users` | Create user |
| `GET` | `/users/{id}` | Get user by ID |
| `PUT` | `/users/{id}` | Update user profile + role |
| `DELETE` | `/users/{id}` | Delete user |
| `PATCH` | `/users/{id}/status` | Enable / disable account |
| `POST` | `/users/{id}/reset-password` | Admin resets password (forces change on next login) |

### Response envelope

All responses follow a consistent JSON envelope:

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

Error responses:

```json
{
  "timestamp": "2026-04-04T20:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: Email must be a valid email address",
  "path": "/api/v1/users"
}
```

### HTTP status codes used

| Code | Meaning |
|------|---------|
| `200` | OK |
| `201` | Created |
| `400` | Validation / bad request |
| `401` | Unauthenticated |
| `403` | Forbidden (insufficient role) |
| `404` | Resource not found |
| `409` | Conflict (duplicate email, role in use, etc.) |
| `500` | Unexpected server error |

---

## Default Credentials

Created automatically by `DataSeeder` on first startup if the admin user does not exist.

| Field | Value |
|-------|-------|
| Email | `admin@stepcore.com` |
| Password | `Admin@2026!` |
| Role | `ADMIN` |

> **Change the password immediately** in production environments.

---

## Running Tests

### Unit tests only (no Docker required)

```bash
mvn test -Dtest="JwtServiceTest,AuthServiceTest,RoleServiceTest,UserServiceTest"
```

### All tests including integration (Docker required)

Integration tests use **Testcontainers** — Docker must be running.

```bash
mvn verify
```

### Coverage report

After running tests, the JaCoCo HTML report is at:

```
target/site/jacoco/index.html
```

Coverage threshold: **≥ 90%** lines and branches (enforced by the `check` goal).

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `stepcore_security` | Database name |
| `DB_USER` | `gmm_user` | Database username |
| `DB_PASSWORD` | `gmm_pass` | Database password |
| `JWT_SECRET` | *(dev default)* | HMAC-SHA256 signing secret (min 32 chars) |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in ms (default: 24 h) |
| `PORT` | `8080` | HTTP server port |

> In production, always set `JWT_SECRET` to a cryptographically random 256-bit value.

---

## Security Model

- **Stateless JWT** — no server-side session storage. Tokens are HMAC-SHA256 signed.
- **BCrypt strength 12** — all passwords hashed before persistence.
- **Password policy** — `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[\W_]).{8,}$`
- **RBAC** — `@PreAuthorize("hasRole('ADMIN')")` on admin-only endpoints.
- **Audit log** — every sensitive action (create/update/delete user, status change, password reset) is recorded in `audit_logs`.
- **CORS / CSRF** — CSRF disabled (stateless); configure CORS per deployment environment.

---

## Contributing

This project follows the standards defined in `ai-specs/specs/`:

- `base-standards.mdc` — core principles (TDD, English-only, incremental changes)
- `backend-standards-java.mdc` — Java/Spring Boot coding standards
- `frontend-standards.mdc` — React/TypeScript frontend standards

All code, variables, comments, logs, and SQL column names must be written in **English**.
