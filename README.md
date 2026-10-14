# kairos-backend

REST API and PostgreSQL persistence for **Kairos**, the athlete performance app. The React frontend lives in a separate repository (`kairos-frontend`).

Java 21 · Spring Boot 3.5 · Spring Security (JWT bearer tokens) · Spring Data JPA · PostgreSQL · Flyway · springdoc OpenAPI · JUnit with an embedded PostgreSQL server.

## What it does

- **Accounts:** register and sign in with email and password (BCrypt-hashed). Sign-in returns a signed access token (HS256 JWT, 12 hours by default). There is no refresh token, password reset or email verification yet.
- **Athlete records**, each private to the signed-in athlete:
  - profile
  - training sessions → exercises → sets
  - one recovery check-in per day
  - one nutrition day per date, with its meals and targets
- **Stored observations only.** Recovery score, training load, intensity, day totals, estimated 1RMs, PRs, readiness and insights are calculated by the frontend (`src/data/domain/`) from these records, so the formulas live in one place and apply to all history. Each formula is documented there and on the page that shows it.
- **No AI and no wearable integrations.** Nothing in the API claims otherwise; recovery records only accept `source.provider = "manual"` until a real provider integration exists.

## API

All endpoints except health, register, login and the docs need `Authorization: Bearer <token>`. Errors are RFC 7807 problem documents; validation errors list each field under `errors`.

| Method | Path | Notes |
|---|---|---|
| GET | `/health` | `{"status":"UP","database":"UP"}`; used by Render's health check |
| POST | `/api/v1/auth/register` | `201` with `{token, expiresAt, user}`; `409` if the email exists |
| POST | `/api/v1/auth/login` | `200` with a token; `401` for any wrong email/password combination |
| GET | `/api/v1/auth/me` | The token's account |
| GET / PUT | `/api/v1/profile` | `404` until saved |
| GET | `/api/v1/training-sessions?from=&to=` | Oldest first; optional date range |
| POST | `/api/v1/training-sessions` | `201` when created; `200` with the stored session if this client `id` already exists (idempotent) |
| DELETE | `/api/v1/training-sessions/{id}` | `204`; `404` if not yours or not found |
| GET | `/api/v1/recovery-records?from=&to=` | |
| PUT / DELETE | `/api/v1/recovery-records/{date}` | One per day; PUT replaces |
| GET | `/api/v1/nutrition-days?from=&to=` | |
| PUT / DELETE | `/api/v1/nutrition-days/{date}` | The whole day including meals; PUT replaces |

The interactive documentation is at `/swagger-ui.html` and the OpenAPI document at `/v3/api-docs`.

Dates are the athlete's local calendar day (`YYYY-MM-DD`) and are stored without time-zone conversion, so a record logged at 23:30 stays on that day. The server rejects only dates that cannot yet be "today" anywhere in the world.

## Run locally

You need a JDK 21 or newer. Maven and PostgreSQL are **not** required.

```bash
./mvnw test                 # all tests, against a real PostgreSQL started from embedded binaries
./mvnw spring-boot:test-run # the API on http://localhost:8080 with a local PostgreSQL
```

`spring-boot:test-run` starts `LocalDevServer` (test sources only). It runs PostgreSQL 17 on port 54329, keeps its data in `.local-dev/` so records survive restarts, and generates a local signing key there. On first start it seeds one demo account: `demo@kairos.local` / `kairos-local-demo`. That seed exists only in the `local` profile and is never packaged.

To run the production jar against your own PostgreSQL, set the variables in `.env.example`, then:

```bash
./mvnw package
java -jar target/kairos-backend.jar
```

The app refuses to start without `KAIROS_JWT_SECRET` (at least 32 bytes).

## Deploy to Render

`render.yaml` is a Render Blueprint: a Docker web service plus a PostgreSQL 17 database. Both are on Render's **free** plans; read the cost notes at the top of that file before relying on it for real data.

1. Push this repository to GitHub.
2. In Render: **New → Blueprint**, then pick the repository. Render creates `kairos-db` and `kairos-backend` and generates `KAIROS_JWT_SECRET`.
3. When prompted, set `KAIROS_CORS_ORIGINS` to the frontend's production URL (for example `https://<project>.vercel.app`). Separate several origins with commas, without trailing slashes.
4. Wait for the deploy, then open `https://<service>.onrender.com/health`. It should return `{"status":"UP","database":"UP"}`.

Flyway applies migrations on startup and Hibernate only validates the schema; it never alters it.

## Project layout

```
src/main/java/com/kairos/backend/
  auth/        accounts, tokens, @CurrentUser
  profile/     athlete profile
  training/    sessions, exercises, sets, exercise catalog
  recovery/    daily recovery check-ins
  nutrition/   nutrition days and meals
  health/      /health
  config/      security, CORS, OpenAPI, settings
  common/      errors, date rules, client ids
src/main/resources/db/migration/   Flyway migrations (V2 is the exercise catalog, generated from the frontend library)
src/test/java/…/dev/                local runner and development seed (never packaged)
```
