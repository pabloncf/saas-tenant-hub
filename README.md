# SaaS Tenant Hub

A production-grade multi-tenant SaaS dashboard built as a portfolio project to demonstrate end-to-end ownership of a secure, scalable backend system.

**Audience:** Recruiters and tech leads in European and American markets looking for backend engineers with a security mindset and full-product ownership capability.

---

## What this project demonstrates

- **Multi-tenancy at the database level** — Row Level Security (RLS) in PostgreSQL enforces tenant isolation without application-layer filtering. Tenant A physically cannot read Tenant B's rows, even if the application has a bug.
- **JWT auth with refresh token rotation** — short-lived access tokens + single-use refresh tokens that rotate on each use, invalidating stolen tokens.
- **Stripe Billing integration** — Checkout Sessions, Customer Portal, and webhook handler with signature verification and idempotency guard against replayed events.
- **RBAC** — four roles (OWNER / ADMIN / MEMBER / VIEWER) enforced via Spring Security `@PreAuthorize` expressions, not ad-hoc `if` checks.
- **Subscription tier enforcement** — FREE / PRO / ENTERPRISE limits are checked at the service layer with a 402 response when exceeded; limits live in the enum to require intentional code changes, not operator config drift.
- **Structured audit trail** — every critical action is logged with tenant context and user identity for compliance and debugging.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16 with Row Level Security |
| Auth | JWT (JJWT), refresh token rotation |
| Billing | Stripe SDK (Checkout, Portal, Webhooks) |
| Migrations | Flyway |
| Frontend | Angular 17+, TypeScript, Chart.js (ng2-charts) |
| Containerization | Docker, Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Angular SPA                                                 │
│  Auth · Dashboard · Members · Billing                        │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTPS / JWT
┌───────────────────────▼─────────────────────────────────────┐
│  Spring Boot API  (/api/v1/*)                                │
│                                                              │
│  TenantFilter → sets app.current_tenant on each request      │
│                                                              │
│  ┌────────────┐  ┌────────────┐  ┌──────────┐  ┌─────────┐ │
│  │    Auth    │  │  Projects  │  │ Members  │  │ Billing │ │
│  │  register  │  │   CRUD +   │  │  RBAC +  │  │ Stripe  │ │
│  │  login     │  │ activity   │  │  invite  │  │ webhook │ │
│  └────────────┘  └────────────┘  └──────────┘  └─────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │ JDBC (two datasources)
┌───────────────────────▼─────────────────────────────────────┐
│  PostgreSQL 16                                               │
│                                                              │
│  saas_app (limited) ──► RLS policies filter every query      │
│  saas_user (admin)  ──► bypasses RLS for auth + webhooks     │
│                                                              │
│  organizations · users · projects · members · activity_logs  │
│  stripe_events (idempotency log, no RLS)                     │
└─────────────────────────────────────────────────────────────┘
```

### Multi-tenancy design

Every table has a `tenant_id` column. A PostgreSQL RLS policy on each table restricts rows to `tenant_id = current_setting('app.current_tenant')`. The Spring `TenantFilter` sets this variable via `SET LOCAL` at the start of each request.

Two datasources handle the split:
- **`saas_app`** — limited-privilege user, RLS active. Used for all tenant-scoped queries.
- **`saas_user`** (admin) — superuser, bypasses RLS. Used only for auth operations (user lookup across tenants) and Stripe webhook processing (tenant context not available from Stripe callbacks).

---

## API Overview

All endpoints live under `/api/v1/`. Responses follow `{ data, meta, errors }` envelope. Full docs available at `/swagger-ui.html` when running.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register org + owner user |
| POST | `/auth/login` | Public | Login, returns JWT pair |
| POST | `/auth/refresh` | Public | Rotate refresh token |
| GET | `/projects` | Any role | List projects (paginated) |
| POST | `/projects` | ADMIN+ | Create project |
| PUT | `/projects/{id}` | ADMIN+ | Update project |
| DELETE | `/projects/{id}` | OWNER | Delete project |
| GET | `/members` | Any role | List members |
| POST | `/members/invite` | ADMIN+ | Invite member |
| PUT | `/members/{id}/role` | OWNER | Change member role |
| DELETE | `/members/{id}` | ADMIN+ | Remove member |
| GET | `/billing/status` | Any role | Current plan + limits |
| POST | `/billing/checkout` | ADMIN+ | Create Stripe Checkout Session |
| POST | `/billing/portal` | ADMIN+ | Create Stripe Customer Portal session |
| POST | `/billing/webhook` | Public* | Stripe webhook receiver |

*Webhook endpoint is unauthenticated but requires valid Stripe-Signature header.

---

## Subscription Tiers

| Tier | Projects | Members |
|---|---|---|
| FREE | 3 | 5 |
| PRO | 50 | 25 |
| ENTERPRISE | Unlimited | Unlimited |

Limits are enforced at `ProjectService.create()` and `MemberService.invite()`. Exceeding a limit returns HTTP 402.

---

## Security Highlights

- **RLS** — isolation enforced at DB layer, not just application layer
- **Refresh token rotation** — stolen refresh tokens are invalidated on next use
- **Stripe webhook signature** — `Webhook.constructEvent` rejects tampered payloads
- **Idempotency on webhooks** — `stripe_events` table deduplicates replayed events
- **No entity exposure** — all API responses use DTOs; JPA entities never cross the API boundary
- **Centralized exception handling** — `@ControllerAdvice` with structured error envelope; no stack traces to clients
- **Audit trail** — `ActivityLog` records project and member actions with tenant + user context

---

## Local Setup

**Prerequisites:** Docker, Docker Compose

```bash
# Clone and start
git clone https://github.com/pabloncf/saas-tenant-hub.git
cd saas-tenant-hub

# Copy and fill environment variables
cp .env.example .env

# Start everything
docker compose up -d

# Backend is available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
# pgAdmin at http://localhost:5050
```

**Backend only (with hot reload):**
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Frontend only:**
```bash
cd frontend
npm install
npm start
# Available at http://localhost:4200
```

**Test accounts (seeded on first run) — all passwords: `Admin@123`**

| Email | Role | Organization | Plan |
|---|---|---|---|
| `admin@demo.com` | OWNER | Demo Organization | FREE |
| `owner@acme.com` | OWNER | Acme Corp | PRO |
| `dev@acme.com` | MEMBER | Acme Corp | PRO |
| `viewer@acme.com` | VIEWER | Acme Corp | PRO |
| `owner@globex.com` | OWNER | Globex Inc | FREE |
| `admin@globex.com` | ADMIN | Globex Inc | FREE |

**Run tests:**
```bash
cd backend
./mvnw test                          # unit tests
./mvnw test jacoco:report            # with coverage report
```

> Integration tests require Docker (Testcontainers spins up a real PostgreSQL instance).

---

## Environment Variables

```env
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=saas_dashboard
POSTGRES_USER=saas_user
POSTGRES_PASSWORD=<secret>

# JWT
JWT_SECRET=<secret-min-256-bits>
JWT_EXPIRATION=900000           # 15 minutes
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRICE_ID_PRO=price_...
STRIPE_PRICE_ID_ENTERPRISE=price_...

# App
APP_BASE_URL=http://localhost:4200
```

---

## License

MIT
