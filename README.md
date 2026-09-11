<div align="center">

# 🔐 Auth + Notification Platform

### Centralized auth (JWT rotation, OAuth2, reset) with async Kafka notifications — modular monolith

**Java 21 + Spring Boot 3.2 + Spring Security + JWT + OAuth2 + Kafka + PostgreSQL + Flyway + Docker**

</div>

---

## Overview

Single-service auth backbone: email verification, short-lived JWT access + rotating refresh tokens in httpOnly-ready flow, Google OAuth2, single-use password reset, per-device sessions, login rate limiting with audit log, and Kafka-driven email + inbox notifications. One Postgres database migrated with Flyway V1→V3, one deployable jar.

### Module map (modular monolith)

```
com.authplatform
├── auth/           # register, verify, login, refresh, forgot/reset, profile, roles
├── session/        # refresh-token issue/rotate/revoke, device sessions, cleanup job
├── security/       # JWT, filter, SecurityConfig, OAuth2 handler, rate limit, audit
├── notification/   # auth-events topic, producer, consumer, inbox API, mail
└── common/         # global exception handler
```

Cross-module rule: modules call each other only through service methods/events, never repositories.

---

## Features

- 📝 Register + 24h email verification link
- 🔑 Login → 15-min JWT access + 7-day rotating refresh token (rotation revokes the old one)
- 🔄 Refresh endpoint, per-device session list, revoke one / revoke all
- 🌐 Google OAuth2 login (auto-provisions verified users)
- 📧 Forgot/reset with single-use 1h tokens (identical responses stop email enumeration)
- 🛡️ Rate limiting (5 attempts → 15-min lockout) + login audit log
- 👑 Admin user list + role promotion endpoint (no self-escalation)
- 📬 Kafka `auth-events` (registered, verified, reset, changed) → inbox rows + email (demo-logged or MailHog)
- 🔔 Notification inbox API with unread count
- 🧹 Hourly cleanup job for expired tokens/audit rows
- 📖 Swagger `/swagger-ui.html`, Actuator health, global error format

---

## API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | — | Register (sends verification event) |
| `POST` | `/api/auth/verify` | — | Verify email with token |
| `POST` | `/api/auth/login` | — | Login → access + refresh |
| `POST` | `/api/auth/refresh` | — | Rotate refresh → new pair |
| `POST` | `/api/auth/forgot` | — | Request reset (always 200) |
| `POST` | `/api/auth/reset` | — | Reset with token, revokes sessions |
| `PUT` | `/api/auth/profile` | ✅ | Edit own name |
| `PUT` | `/api/auth/password` | ✅ | Change password, revokes others |
| `GET` | `/api/sessions` | ✅ | List my devices |
| `DELETE` | `/api/sessions/{id}` | ✅ | Revoke one |
| `DELETE` | `/api/sessions` | ✅ | Revoke all |
| `GET` | `/api/notifications` | ✅ | Inbox |
| `GET` | `/api/notifications/unread-count` | ✅ | Unread badge |
| `PUT` | `/api/notifications/{id}/read` | ✅ | Mark read |
| `GET` | `/api/admin/users` | 🔒 Admin | User list |
| `PUT` | `/api/admin/role` | 🔒 Admin | Promote/demote |

---

## Getting Started

### Prerequisites
- Java 21+, Node.js 18+, Docker & Docker Compose, Maven 3.9+

```bash
git clone https://github.com/<your-username>/Auth-Notification-Service.git
cd Auth-Notification-Service
cp .env.example .env
docker compose up --build -d
```

| Service | URL |
|---|---|
| API | http://localhost:8090 |
| Swagger | http://localhost:8090/swagger-ui.html |
| MailHog (real emails in docker) | http://localhost:8026 |
| Frontend (dev) | http://localhost:5174 |

Admin: register, then `UPDATE users SET role='ROLE_ADMIN' WHERE email='...';` on `:5433`.

### Tests
```bash
./mvnw test "-Dtest=AuthServiceTest,RateLimitServiceTest,NotificationConsumerTest"  # unit, no Docker needed
./mvnw test "-Dtest=AuthFlowIntegrationTest" "-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition"  # needs Linux Docker host (runs in CI)
```

---

## Frontend

`auth-ui/` — minimal React app (login, register, verify, reset, sessions, inbox) sharing the same Tailwind system.

---

## License

MIT
