# LegacyLoop — project skeleton

Student and alumni networking + placement platform. This repository is the **skeleton**: the
build files, shared code, configuration and routing are done, and every feature file is an empty
stub waiting for the member who owns it.

Clone it, run it, and you get three services that start and a web app that renders. Nothing else
works yet — that is the point.

## What is already built (do not rewrite)

| Piece | Where | Why it is here |
|---|---|---|
| Parent POM, service POMs, Dockerfiles | `backend/` | so `mvn package` works on day one |
| `common` module | `backend/common` | `ApiResponse`, `ApiException`, `ErrorCode`, `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `GlobalExceptionHandler`, `Roles`, RabbitMQ config |
| API gateway | `backend/api-gateway` | one entry point on `:8080`, routes by path prefix |
| Service configuration | `*/src/main/resources/application.yml` | database, JWT, mail, logging |
| docker-compose | `docker-compose.yml` | MySQL, MongoDB, RabbitMQ, three services, web app |
| Frontend shell | `frontend/src/{main.jsx, App.jsx, index.css, lib/, components/}` | routing, `api.js` with every endpoint, auth context, token refresh, toasts, layout, design system |

## What you write

Everything under a `controller/`, `service/`, `repository/`, `entity/`, `dto/` package, and every
file in `frontend/src/pages/`. Each one has a `package-info.java` or a header comment naming the
owner and what belongs there.

**File paths and names match the working project exactly**, so a finished file pastes straight
over its stub and every import — `../lib/api`, `../lib/auth`, `../lib/toast`,
`../components/ui`, `com.legacyloop.common.*` — still resolves. Nothing to rename, no import to
fix up.

Who owns what: [MODULES.md](MODULES.md). How to branch and push: [CONTRIBUTING.md](CONTRIBUTING.md).
The endpoint contract: [docs/API.md](docs/API.md).

## Run it

```bash
cp .env.example .env          # defaults work for a local run

# infrastructure
docker compose up -d mysql mongodb rabbitmq

# backend (from the backend directory)
cd backend && mvn clean install -DskipTests
mvn -pl user-service spring-boot:run      # :8081, and the same for career/social

# frontend
cd frontend && npm install && npm run dev  # :5173, proxies /api to :8080
```

Everything at once, in Docker:

```bash
docker compose up --build
```

Check it is alive:

```bash
curl http://localhost:8081/internal/health
curl http://localhost:8082/internal/health
curl http://localhost:8083/internal/health
```

## Stack

Java 21, Spring Boot 3.3, Spring Cloud Gateway, Spring Security + JWT, JPA/MySQL (user, career),
MongoDB (social), RabbitMQ, springdoc/Swagger, JUnit 5.

**React 19.2**, Vite 8 with `@vitejs/plugin-react` 6, Tailwind 4, React Router 7, TanStack Query 5,
axios, Recharts 3, lucide-react 1, `@stomp/stompjs` 7 — current versions, all React 19 compatible.
`.npmrc` sets `legacy-peer-deps=true` so a lagging transitive peer range cannot fail your install.

Ports: gateway 8080, user 8081, career 8082, social 8083, web 5173 (dev) / 3000 (Docker).
Swagger per service at `http://localhost:<port>/swagger-ui.html`.
