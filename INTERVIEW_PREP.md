# Interview Preparation — Smart Expense Tracker & Advisor

## STAR Answer: "Tell me about your Smart Expense Tracker & Advisor project"

**Situation:** I wanted a portfolio project that went beyond a basic CRUD app and actually demonstrated backend engineering — proper security, caching, and a real integration with an LLM — rather than just another to-do list clone.

**Task:** I set out to build a personal finance tracker where users can log expenses, set budgets, and get AI-generated financial advice, with production-grade concerns handled from day one: authentication, per-user data isolation, caching, and deployment.

**Action:** I built the backend in Java and Spring Boot as a modular monolith — Controller, Service, Repository layers backed by MySQL through JPA/Hibernate. Every expense and budget endpoint is scoped to the authenticated user via a JWT issued at login, so a user can never read or modify another user's data, even if they guess a valid ID. All the financial calculations — totals, category percentages, month-over-month change, budget utilization — happen in Java; nothing is delegated to an LLM for arithmetic. For the AI Financial Advisor feature, I send those already-computed numbers to Google's Gemini API from the backend, and Gemini's only job is to turn them into natural-language recommendations — the API key never touches the frontend. Analytics endpoints are cached in Redis with a cache-aside pattern and a sensible TTL, invalidated whenever an expense or budget changes, and the app falls back to MySQL gracefully if Redis is ever unavailable. On the frontend, I built a React and TypeScript dashboard with Tailwind, covering loading, empty, and error states throughout so it never looks broken. The whole thing is containerized with Docker Compose and configured entirely through environment variables so it's deployment-ready rather than localhost-only.

**Result:** I ended up with a working, deployable full-stack app that exercises Spring Security, JPA, Redis caching, and a real AI integration — the kind of decisions I'd actually have to make and defend on the job, not just a keyword checklist.

---

## Resume Bullets

- Built a full-stack expense-tracking application (Java 17, Spring Boot, MySQL, React/TypeScript) with JWT-based authentication and per-user data isolation enforced at the repository layer.
- Designed and implemented Redis-backed caching for analytics endpoints using a cache-aside pattern with TTL and write-triggered invalidation, with graceful fallback to MySQL on cache unavailability.
- Integrated Google's Gemini API for an AI financial advisor feature, keeping all financial calculations in the Java backend and using the LLM strictly for natural-language generation from precomputed data.
- Containerized the application with Docker and Docker Compose, configuring the stack (Spring Boot, MySQL, Redis) entirely through environment variables for cloud deployment.

---

## General Interview Q&A

### Java
1. **What are the four pillars of OOP?** Encapsulation, abstraction, inheritance, polymorphism — I used encapsulation heavily (entities expose behavior via getters/setters, never raw internal state to the API layer via DTOs) and polymorphism through Spring's interface-based dependency injection.
2. **Why use `Optional`?** To make the possible absence of a value explicit in the type system instead of relying on nulls — I use it in repository lookups like `findByIdAndUserId`.
3. **Streams vs loops?** Streams express *what* to compute (filter, map, reduce) rather than *how* to iterate — I use them for category totals and percentage calculations in `AnalyticsService`.
4. **Checked vs unchecked exceptions?** Checked exceptions must be declared/caught (e.g., `IOException`); unchecked (`RuntimeException` subclasses) don't have to be. My custom exceptions (`ResourceNotFoundException`, etc.) are unchecked so service code stays clean, and `@RestControllerAdvice` handles translation to HTTP responses centrally.
5. **What's a record, and why use one?** An immutable data carrier class with generated constructor/equals/hashCode/toString — I used records for all DTOs since they're pure data with no behavior.

### Spring Boot
6. **What is Inversion of Control?** The framework, not your code, controls object creation and wiring — you declare dependencies and Spring provides them.
7. **What is Dependency Injection, and how did you use it?** Constructor injection via Lombok's `@RequiredArgsConstructor` — every service/controller declares its dependencies as `final` fields and Spring injects them, which also makes testing with Mockito straightforward.
8. **What's a Spring Bean?** An object managed by the Spring IoC container — created via `@Component`, `@Service`, `@Repository`, `@Configuration` classes with `@Bean` methods, etc.
9. **Difference between `@Component`, `@Service`, `@Repository`?** Functionally similar (all are beans); the differences are semantic/documentation, though `@Repository` also enables exception translation for persistence exceptions.
10. **What do Spring profiles do?** Let you swap configuration by environment — I use `application-dev.properties` (SQL logging on, debug logs) vs `application-prod.properties` (no stack traces, minimal logging) selected via `SPRING_PROFILES_ACTIVE`.

### REST
11. **What HTTP status codes does your API use, and why?** 200 for successful reads/updates, 201 for creation, 204 for deletion (no body), 400 for validation errors, 401 for bad credentials, 403 for forbidden access, 404 for missing/not-owned resources, 409 for duplicate email on registration, 503 if Gemini is down.
12. **Why DTOs instead of exposing entities directly?** Decouples the API contract from the database schema, avoids leaking JPA proxy/lazy-loading artifacts into JSON, and lets me control exactly what a client sees (e.g., never expose another user's data by accident through an over-eager `@ManyToOne` serialization).
13. **How does pagination work in your API?** Spring Data's `Pageable`/`Page<T>` — the client sends `page`, `size`, `sort` query params, and the repository returns only that page's rows via a `LIMIT`/`OFFSET` query, not the whole table.
14. **How do you validate input?** Jakarta Bean Validation annotations (`@NotNull`, `@Positive`, `@Email`, `@Size`) on request DTOs, with `@Valid` in the controller and a `@RestControllerAdvice` handler that collects field errors into one message.

### Spring Security / JWT
15. **Walk me through your JWT flow.** On login, `AuthService` verifies the BCrypt-hashed password and calls `JwtUtil.generateToken(userId, email)`, which signs a token with an HMAC-SHA256 key from an env var. The client stores it and sends it as `Authorization: Bearer <token>`. `JwtAuthFilter` runs once per request, validates the signature/expiry, and if valid, puts a `UserPrincipal(userId, email)` into the `SecurityContext` — no server-side session state at all.
16. **Why BCrypt over plain hashing (e.g. SHA-256)?** BCrypt is intentionally slow and includes a per-password salt, making brute-force and rainbow-table attacks impractical; a fast hash like SHA-256 is designed for speed, which is the opposite of what you want for passwords.
17. **How do you prevent User A from accessing User B's data?** Every expense/budget query is scoped by `userId` at the repository level (`findByIdAndUserId`), not just filtered after fetching — so it's structurally impossible to load another user's row even by guessing IDs. A missing-or-not-owned resource returns the same 404 either way, so an attacker can't even tell whether an ID exists.
18. **What does "stateless authentication" mean, and why does it matter here?** No session is stored server-side; every request carries its own proof of identity (the JWT). This makes horizontal scaling trivial — any backend instance can validate a token without shared session storage.

### JPA / Hibernate
19. **What's the N+1 query problem, and did you hit it?** Fetching a list of entities, then lazily loading an association per row (N extra queries). I used aggregation queries (`GROUP BY` in JPQL) for analytics instead of loading entities and looping in Java, which avoids this entirely for the expensive endpoints.
20. **Lazy vs eager loading?** Lazy defers loading an association until accessed; eager loads it immediately. I use `FetchType.LAZY` on `Expense.category` and `Expense.user` and set `spring.jpa.open-in-view=false` so lazy loading can't silently happen inside view rendering — DTOs explicitly pull what they need inside the transaction.
21. **What's the persistence context?** Hibernate's first-level cache/unit-of-work per transaction — entities loaded within it are tracked, and changes are flushed automatically at commit (dirty checking).
22. **How do you avoid over-normalizing?** Budgets store a nullable `category_id` rather than separate "overall budget" and "category budget" tables — one entity, one nullable FK, less join complexity for a distinction that isn't structurally different.

### MySQL
23. **What indexes did you add, and why?** `user_id`, `expense_date`, and `category_id` on `expenses` — those are exactly the columns every analytics query filters or groups on, so without them those queries would be full table scans as data grows.
24. **How do you calculate category totals efficiently?** A single `GROUP BY category_id` query in JPQL, not "fetch all expenses, group in Java" — push the aggregation to the database.
25. **Why a unique constraint on `(user_id, year, month, category_id)` for budgets?** Guarantees at the DB level that a user can't have two conflicting budgets for the same category in the same month — an invariant that's cheap to enforce there and expensive to guarantee only in application code.

### Redis
26. **Why Redis here — what's actually being cached?** The analytics endpoints (`/monthly`, `/category`, `/budget`) — they run aggregation queries over potentially many rows, and the same numbers get requested repeatedly (every dashboard load) between changes, so caching them avoids recomputing the same aggregation on every page view.
27. **Walk through a cache miss.** Request comes in → `AnalyticsCacheService.getOrCompute` checks Redis by key → not found → calls the supplier, which runs the real MySQL aggregation query → result is written back to Redis with a 10-minute TTL → returned to the caller.
28. **How do you invalidate the cache?** On any expense or budget create/update/delete, the service calls `cacheService.evict("analytics:{userId}")`, which deletes all Redis keys under that prefix — so the next read is guaranteed fresh rather than relying on TTL expiry alone.
29. **What happens if Redis goes down?** Every cache operation is wrapped in try/catch; a failure is logged as a warning and the code falls straight through to the MySQL calculation instead of throwing — Redis is purely an optimization, never a hard dependency. I have a unit test asserting this fallback path explicitly.
30. **Redis vs MySQL — why not just rely on MySQL's own query cache?** Redis is a dedicated in-memory store, controllable per-key with explicit TTLs and explicit invalidation logic that I own, rather than being subject to MySQL's own (and largely deprecated) caching internals.

### Gemini / AI
31. **What is Gemini, and why use it here?** Google's LLM API family; I use it purely for natural-language generation of financial advice — not calculation.
32. **How does Spring Boot talk to Gemini?** `GeminiClient` POSTs to Gemini's `generateContent` REST endpoint with the API key as a query param, using Spring's `RestClient`; the response JSON is parsed for the generated text.
33. **What data gets sent to Gemini?** Only already-computed numbers — total spending, category totals/percentages, budget utilization, month-over-month change — formatted into a structured prompt. No raw PII beyond what's needed for the numbers themselves.
34. **Why doesn't Gemini calculate the financial numbers?** LLM arithmetic isn't reliably precise, and a user's budget figures need to be exactly correct, not "usually right." Java does 100% of the math; Gemini only writes the prose around numbers it's given.
35. **How is the Gemini API key protected?** It's read from an environment variable (`GEMINI_API_KEY`) on the backend only, injected into `GeminiClient`'s constructor — it's never included in any response sent to the React frontend, so it's not visible in browser dev tools or network tabs.
36. **What happens if Gemini is unavailable?** `GeminiClient` catches the failure and throws `ApiExceptions.AiServiceException`, which `GlobalExceptionHandler` turns into a 503 with a friendly message. The frontend's AI Advisor page shows an error state with a retry button instead of crashing.

### Docker & Deployment
37. **Why Docker?** Consistent environment between my machine, CI, and production — MySQL/Redis versions, Java version, and app config are all pinned and reproducible rather than "works on my machine."
38. **What does your docker-compose.yml orchestrate?** MySQL, Redis, the Spring Boot backend, and the React frontend (served via nginx), networked together with healthchecks so the backend waits for MySQL/Redis to actually be ready before starting.
39. **How does container-to-container networking differ from localhost?** Containers reach each other by service name on the Compose network (e.g., `jdbc:mysql://mysql:3306/...`), not `localhost` — `localhost` inside a container refers to that container itself, not the host machine or sibling containers.
40. **How does your app differ between dev and production config?** Dev enables SQL logging and debug-level logs; prod disables stack traces and verbose logging and sets `ddl-auto` conservatively. Both are driven by the same environment-variable-based `application.properties`, just with a different active profile.
41. **How does CORS work in your setup?** `SecurityConfig` builds a `CorsConfigurationSource` allowing only the configured `FRONTEND_URL` origin, with specific methods and headers — not a wildcard `*`, so a browser will only let scripts from that origin actually read the API's responses.

### Project-Specific
42. **What was the most difficult part?** Getting the cache invalidation and per-user cache keys right — the cache key includes the user ID so caching never leaks data across users, and every mutation path (create/update/delete expense, create/delete budget) had to remember to evict.
43. **What would you improve in v2?** Refresh tokens instead of one long-lived JWT, recurring expense detection, and moving budget-threshold alerts to a push notification instead of requiring the user to open the app.
44. **How did you test the Redis caching logic?** Unit tests with Mockito mocking `RedisTemplate` — one test asserts a cache hit skips the supplier, one asserts a miss calls the supplier and writes back, and one asserts that when `RedisTemplate` throws, the supplier's value is still returned rather than the exception propagating.
45. **How did you test the AI service?** By mocking `GeminiClient` in `GeminiFinancialAdvisorServiceTest` — one test verifies the prompt-driven flow returns Gemini's text on success, another verifies that when `GeminiClient` throws, the exception propagates as `AiServiceException` so the global handler can convert it to a 503.
46. **Why did you choose MySQL over Postgres or a NoSQL store?** The data is inherently relational (users → expenses/budgets → categories, with real foreign-key constraints and aggregation-heavy queries) — a relational DB with strong consistency guarantees is the right fit; there's no schema-flexibility need that would justify NoSQL here.
47. **How do you handle a validation error end-to-end?** Jakarta annotations on the DTO trigger a `MethodArgumentNotValidException`, caught centrally by `GlobalExceptionHandler`, which collects field errors into one readable message and returns 400 — the frontend displays that message directly in the form.
48. **Explain your budget status thresholds.** `utilization >= 100` → `EXCEEDED`, `>= 80` → `APPROACHING`, else `NORMAL` — computed once in `BudgetService.toResponse` so both the direct budget endpoints and the AI Advisor's prompt-building see the same numbers.
49. **How does the dashboard avoid loading the entire expense table?** It never queries expenses directly for totals — analytics endpoints run `SUM`/`GROUP BY` aggregation queries in the database, and the "recent transactions" list uses `Pageable` to fetch only 5 rows.
50. **Why a modular monolith instead of microservices?** At this scale, microservices would add network overhead, deployment complexity, and distributed-transaction headaches without buying anything — a single well-layered Spring Boot app already separates concerns cleanly (controller/service/repository/ai/cache) and is far easier to reason about, test, and deploy.
51. **What's your rollback story if a database migration goes wrong?** Honestly, this project uses `ddl-auto=update` for simplicity rather than a migration tool like Flyway/Liquibase — a real production system would need versioned migrations with rollback scripts, which I'd add before this went to real users.

*(Note: performance numbers, user counts, and benchmarks are intentionally not claimed anywhere in this document — they weren't measured, so I'm not fabricating them for an interview.)*
