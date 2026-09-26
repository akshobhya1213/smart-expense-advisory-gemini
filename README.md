# Smart Expense Tracker & Advisor

A full-stack personal finance app: expense tracking, budgeting, financial analytics, and an AI-powered financial advisor built on Google Gemini — with a Java/Spring Boot backend and a React/TypeScript frontend.

**Live demo:** _add your deployed URL here_
**Swagger:** _add your deployed `/swagger-ui/index.html` URL here_

---

## Features

- JWT authentication (register/login) with BCrypt password hashing, stateless sessions
- Full expense CRUD with search, category/date/payment-method filters, sorting, pagination, CSV export
- Category management with sensible defaults (Food, Transportation, Shopping, Bills, Entertainment, Healthcare, Education, Other)
- Monthly and per-category budgets with utilization % and status (NORMAL / APPROACHING / EXCEEDED)
- Backend-computed financial analytics: monthly summary, category breakdown, daily spending trend, budget utilization, month-over-month comparison
- Redis-cached analytics with cache-aside pattern, TTL, and write-triggered invalidation — with graceful fallback to MySQL if Redis is unreachable
- AI Financial Advisor: Java calculates all the numbers; Gemini turns them into natural-language recommendations. The Gemini API key never leaves the backend.
- Premium fintech-style dashboard: donut charts, trend lines, budget cards, recent transactions, loading/empty/error states throughout
- Swagger/OpenAPI docs, global exception handling with no leaked internals, Dockerized for local + production deployment

## Tech Stack

**Backend:** Java 17, Spring Boot 3, Spring Web, Spring Data JPA/Hibernate, Spring Security, JWT (jjwt), BCrypt, MySQL 8, Redis, Maven, JUnit 5 + Mockito, springdoc-openapi

**Frontend:** React 18, TypeScript, React Router, Tailwind CSS, Recharts, Axios, lucide-react

**AI:** Google Gemini API (`gemini-2.0-flash`), called only from the backend

**Infra:** Docker, Docker Compose

## Architecture

```
React + TypeScript  ──HTTPS──>  Spring Boot REST API
                                        │
                         Spring Security + JWT filter
                                        │
                          Controller → Service → Repository
                                        │
                        ┌───────────────┼────────────────┐
                        ▼                                ▼
                     MySQL                            Redis
                (persistent data)              (analytics cache)
                        │
                        ▼
                Google Gemini API
           (natural-language advice only —
            never the source of financial numbers)
```

Modular monolith, layered by responsibility (`config`, `controller`, `dto`, `entity`, `exception`, `repository`, `security`, `service`, `ai`, `cache`) — no microservices, no message queues, no vector DB. That complexity isn't needed here and would only get in the way of explaining the project clearly.

## Database Schema

```
User 1───* Expense        Category 1───* Expense
User 1───* Budget         Category 1───* Budget
```

- `expenses`: indexed on `user_id`, `expense_date`, `category_id`
- `budgets`: unique per `(user_id, year, month, category_id)` — `category_id` nullable = overall monthly budget

## API Overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Expenses | `POST/GET /api/expenses`, `GET/PUT/DELETE /api/expenses/{id}` |
| Categories | `GET /api/categories` |
| Budgets | `POST /api/budgets`, `GET /api/budgets?year=&month=`, `DELETE /api/budgets/{id}` |
| Analytics | `GET /api/analytics/{monthly,category,trends,budget}` |
| AI Advisor | `GET /api/ai/insights` |
| Health | `GET /api/health` |

Full request/response contracts are documented in Swagger at `/swagger-ui/index.html` once the backend is running.

## Redis Caching

Analytics endpoints (`/monthly`, `/category`, `/budget`) follow cache-aside:

```
Request → Redis → HIT → return cached value
Request → Redis → MISS → MySQL → compute → store in Redis (10 min TTL) → return
```

Any expense or budget write evicts that user's `analytics:{userId}:*` keys, so the next read recomputes fresh data. If Redis is unreachable (connection refused, timeout), `AnalyticsCacheService` catches the exception, logs a warning, and falls straight through to MySQL — the app never crashes because Redis is down.

## AI Financial Advisor

```
User → React → Spring Boot
                  │
        Java calculates: totals, category %, budget utilization, MoM change
                  │
        Structured prompt built from those numbers
                  │
              Gemini API
                  │
        Natural-language recommendations
                  │
        Spring Boot → React
```

Gemini is never asked to do arithmetic — it receives pre-computed figures and writes prose around them. If the Gemini call fails (bad key, network issue, quota), the backend returns a 503 with a friendly message instead of crashing, and the frontend shows a retry state.

## Local Setup

### Prerequisites
- Java 17+, Maven 3.9+, Node 20+, MySQL 8+, Redis 7+ (or use Docker Compose for the last two)

### Backend
```bash
cd backend
cp ../.env.example ../.env   # fill in real values
export $(grep -v '^#' ../.env | xargs)   # or use your IDE's env file support
mvn spring-boot:run
```
Runs on `http://localhost:8080`. Swagger: `http://localhost:8080/swagger-ui/index.html`

### Frontend
```bash
cd frontend
npm install
echo "VITE_API_BASE_URL=http://localhost:8080/api" > .env
npm run dev
```
Runs on `http://localhost:5173`

## Docker Setup

```bash
cp .env.example .env   # fill in real values (DB_PASSWORD, JWT_SECRET, GEMINI_API_KEY)
docker compose up --build
```

This starts MySQL, Redis, the Spring Boot backend, and the React frontend (served via nginx) — all networked together using Docker service names (`mysql`, `redis`, `backend`), never `localhost`.

## Environment Variables

See [`.env.example`](.env.example). Never commit a real `.env` — it's gitignored.

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | Token signing |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Cache connection |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | AI Advisor (backend only, never sent to the browser) |
| `FRONTEND_URL` | CORS allow-list |
| `VITE_API_BASE_URL` | Frontend → backend base URL |

## Testing

```bash
cd backend
mvn test
```

Covers: registration/login success & failure, per-user expense authorization (404 on cross-user access), budget utilization/status thresholds, Redis cache hit/miss/fallback-on-failure, and Gemini failure propagation into a friendly error. Run with JUnit 5 + Mockito, MySQL-mode H2 for anything needing a DB context.

## Deployment

- **Frontend** → any static host that supports SPA fallback routing (e.g. Vercel): `npm run build`, deploy `dist/`, set `VITE_API_BASE_URL` to your deployed backend URL.
- **Backend** → any platform that runs a Docker image or a Java jar (e.g. Render, Railway): point it at the `backend/Dockerfile`, set the environment variables above.
- **Database** → managed MySQL (e.g. PlanetScale, Railway MySQL, RDS).
- **Redis** → managed Redis (e.g. Upstash, Railway Redis).
- Set `SPRING_PROFILES_ACTIVE=prod` in production. Update `FRONTEND_URL` to your real deployed frontend origin for CORS.

> Provider tiers and setup steps change often — check the current docs for whichever platform you pick before deploying, rather than relying on this README staying current.

## Future Improvements

- Recurring/subscription expense detection
- Multi-currency support
- Shared/family budgets
- Push notifications when a budget crosses its threshold
- Refresh tokens instead of a single long-lived JWT
