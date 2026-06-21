# Habit Tracker

A streak-based habit tracking web application designed to help users build consistency through daily check-ins, progress visibility, and a gamified Minecraft-inspired interface.

📐 **[Architecture, database schema & feature docs →](https://github.com/LostChessElo/habit-tracker/wiki)**

---

## Project Status

| Area                              | Status         |
| ---------------------------------- | -------------- |
| Backend — Authentication           | ✅ Complete     |
| Backend — Habit CRUD               | ✅ Complete     |
| Backend — Habit Logging & Streaks  | ✅ Complete     |
| Frontend                           | 🔲 Not started |
| Deployment                         | 🔲 Not started |

---

## Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.6 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Database access | Spring JDBC (`NamedParameterJdbcTemplate`) |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Build tool | Maven (Maven Wrapper included) |
| Dev database | Docker Compose (`postgres:16`) |
| Test database | H2 (in-memory) |

### Frontend *(planned)*
| Layer | Technology |
|---|---|
| Framework | React + Vite |
| Styling | Tailwind CSS |
| Routing | React Router |
| HTTP client | Axios |
| UI theme | Minecraft-inspired with pixel-art character |

### Infrastructure *(planned)*
| Service | Provider |
|---|---|
| Database | Neon (serverless PostgreSQL) |
| Backend | Railway |
| Frontend | Vercel |

---

## Architecture

The backend follows a strict layered architecture — every feature has the same shape:

```
Controller  →  Service  →  Repository  →  PostgreSQL
     ↑
JWT filter extracts userId from token,
injects into SecurityContext as principal.
Controller reads it via Authentication.getPrincipal().
UserId never comes from the request body.
```

See the [wiki](https://github.com/LostChessElo/habit-tracker/wiki) for full architecture diagrams, database schema, and per-feature design docs (Authentication, Habits, Habit Logs, Exceptions, Users).

### Package structure

```
com.tracker.habit/
├── auth/
│   ├── AuthController.java        # POST /api/auth/register, /api/auth/login
│   ├── AuthService.java           # BCrypt hashing, JWT generation
│   ├── JwtAuthFilter.java         # Validates Bearer token on every request
│   ├── JwtUtil.java               # Token creation and claim extraction
│   └── dto/
│       └── AuthDtos.java          # RegisterRequest, LoginRequest, AuthResponse
├── config/
│   └── SecurityConfig.java        # Filter chain — /api/auth/** public, all else authenticated
├── exception/
│   ├── ApiException.java          # Runtime exception with HTTP status
│   └── GlobalExceptionHandler.java # @ControllerAdvice — maps exceptions to JSON errors
├── habit/
│   ├── Habit.java                 # Domain record
│   ├── HabitController.java       # GET/POST/PUT/DELETE /api/habits
│   ├── HabitService.java          # Business logic, ownership verification
│   ├── HabitRepository.java       # Raw SQL CRUD via NamedParameterJdbcTemplate
│   └── dto/
│       └── HabitDtos.java         # Create/update/response DTOs
├── habitlog/
│   ├── HabitLog.java              # Domain record
│   ├── HabitLogController.java    # POST/DELETE/GET /api/habits/{id}/log
│   ├── HabitLogService.java       # Streak calculation logic
│   └── HabitLogRepository.java    # Idempotent daily log/unlog
├── user/
│   ├── User.java                  # Domain record
│   └── UserRepository.java        # findByEmail, save
└── HabitApplication.java
```

### Database migrations

Flyway runs on startup and applies migrations in version order:

| Migration | Description |
|---|---|
| `V1__create_users.sql` | `users` table — id, email, password_hash, created_at |
| `V2__create_habits.sql` | `habits` table — id, user_id (FK), name, description, created_at |
| `V3__create_habit_logs.sql` | `habit_logs` table — id, habit_id (FK), completed_on (DATE), unique constraint |

---

## What's Complete

### Authentication
- `POST /api/auth/register` — creates a user with a BCrypt-hashed password, returns a JWT
- `POST /api/auth/login` — validates credentials, returns a JWT
- JWT filter runs on every protected request, extracts `userId` and loads it into the `SecurityContext`
- All routes except `/api/auth/**` require a valid token
- Global exception handler returns consistent JSON error responses with appropriate HTTP status codes

### Habit CRUD
- Full create/read/update/delete for habits, scoped to the authenticated user
- Ownership verification — a user can only access their own habits
- `NamedParameterJdbcTemplate` for raw SQL data access

### Habit Logging & Streaks
- Daily check-in (log) and undo (unlog), idempotent per day
- Streak calculation — consecutive day count walking backwards from today
- Enriched habit responses include streak count and `completedToday` boolean

### CI/CD pipeline
- GitHub Actions workflow runs on every push
- Builds with Maven, runs tests
- Secret scanning via ggshield

### Local development
- `docker-compose.yml` spins up a local PostgreSQL 16 instance
- Spring Boot DevTools configured for auto-restart
- `.env` file used for database credentials (gitignored)

---

## Getting Started

### Prerequisites
- Java 21
- Maven (or use the included `./mvnw`)
- Docker (for local PostgreSQL)

### Running locally

1. Clone the repository:
   ```bash
   git clone https://github.com/LostChessElo/habit-tracker.git
   cd habit-tracker
   ```

2. Create a `.env` file in the project root:
   ```env
   POSTGRES_DB=habitdb
   POSTGRES_USER=your_user
   POSTGRES_PASSWORD=your_password
   JWT_SECRET=your_base64_encoded_secret
   JWT_EXPIRATION=3600000
   ```

   > ⚠️ `JWT_SECRET` **must** be a base64-encoded key of at least 256 bits (32 bytes). The application will fail to start without a valid key of sufficient length. Generate one with:
   > ```bash
   > openssl rand -base64 32
   > ```

3. Start the database:
   ```bash
   docker compose up -d
   ```

4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

   The API will be available at `http://localhost:8080`.

### Running tests

```bash
./mvnw test
```

Tests use an H2 in-memory database — no Docker required.

---

## API Reference

### Authentication

```
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

Both endpoints return:
```json
{
  "token": "<jwt>"
}
```

All subsequent requests require the token as a Bearer header:
```
Authorization: Bearer <token>
```

### Habits

```
GET    /api/habits           # list all habits for the authenticated user
POST   /api/habits           # create a habit
GET    /api/habits/{id}      # get a single habit with streak data
PUT    /api/habits/{id}      # update a habit
DELETE /api/habits/{id}      # delete a habit
```

### Habit Logs

```
POST   /api/habits/{id}/log  # mark habit complete for today
DELETE /api/habits/{id}/log  # unmark habit for today
GET    /api/habits/{id}/log  # get log history
```

For full request/response schemas and design notes, see the [Habits](https://github.com/LostChessElo/habit-tracker/wiki/Habits) and [Habit Logs](https://github.com/LostChessElo/habit-tracker/wiki/Habit-Logs) wiki pages.

---

## Environment Variables

| Variable | Description |
|---|---|
| `POSTGRES_DB` | Database name |
| `POSTGRES_USER` | Database user |
| `POSTGRES_PASSWORD` | Database password |
| `JWT_SECRET` | Base64-encoded signing key, **≥256 bits (32 bytes)**. App fails to start if invalid or too short. |
| `JWT_EXPIRATION` | Token expiry in milliseconds |

---

## Roadmap

- [x] Project foundation & repository setup
- [x] CI/CD pipeline (GitHub Actions + ggshield)
- [x] Authentication (register, login, JWT)
- [x] Habit CRUD API
- [x] Habit logging & streak calculation
- [ ] Frontend scaffold (React + Vite)
- [ ] Minecraft-style UI with cursor-tracking character
- [ ] Deploy — Neon (DB), Railway (API), Vercel (frontend)
