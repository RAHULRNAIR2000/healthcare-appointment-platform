# Healthcare Appointment Platform

A full-stack healthcare appointment management system built as a backend engineer take-home assignment. Patients can register, browse doctors, book appointments, and track real-time status updates powered by an event-driven pipeline.

---

## Architecture

```
React Frontend (Nginx)
        │  HTTP
        ▼
Spring Boot REST API  ──► RabbitMQ ──► Python Worker
        │                                    │
        └──────────────────┬─────────────────┘
                           ▼
                       PostgreSQL
```

**Key design decision:** Spring Boot only sets appointments to `PENDING`. The Python worker is the only component that sets `CONFIRMED` — this proves the async event pipeline is working end to end.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS |
| Backend | Spring Boot 3.2.5, Java 17, Maven |
| Worker | Python 3.11, pika, psycopg2 |
| Database | PostgreSQL 15 (Flyway migrations) |
| Messaging | RabbitMQ 3.12 |
| Auth | Stateless JWT (HS256) |
| Containers | Docker + Docker Compose |

---

## Port Map

| Service | Host Port | Notes |
|---|---|---|
| Frontend (Nginx) | **5173** | React SPA |
| Backend (Spring Boot) | **9090** | REST API + Swagger |
| PostgreSQL | **5433** | Mapped from internal 5432 |
| RabbitMQ AMQP | **5673** | Mapped from internal 5672 |
| RabbitMQ Management UI | **15673** | Admin dashboard |

---

## Quick Start

### Prerequisites
- Docker Desktop (or Docker + Docker Compose on Linux)
- Git

### 1. Clone the repo
```bash
git clone https://github.com/RAHULRNAIR2000/healthcare-appointment-platform.git
cd healthcare-appointment-platform
git checkout stage
```

### 2. Create the environment file
```bash
cp .env.example .env
```
Edit `.env` and set a strong `JWT_SECRET` for production. All other defaults are fine for local use.

### 3. Start everything
```bash
docker compose up -d --build
```

This starts 5 containers: postgres, rabbitmq, backend, worker, and frontend. The `--build` flag builds the Spring Boot JAR and the React app inside Docker — no local Java or Node required.

### 4. Wait for health checks (~30 seconds), then verify
```bash
docker compose ps
```

Expected output:
```
NAME             STATUS      PORTS
hap-postgres     healthy     0.0.0.0:5433->5432/tcp
hap-rabbitmq     healthy     0.0.0.0:5673->5672/tcp, 0.0.0.0:15673->15672/tcp
hap-backend      healthy     0.0.0.0:9090->9090/tcp
hap-worker       running
hap-frontend     running     0.0.0.0:5173->80/tcp
```

### 5. Access the platform

| URL | Description |
|---|---|
| http://localhost:5173 | React frontend |
| http://localhost:9090/swagger-ui.html | API docs (Swagger UI) |
| http://localhost:15673 | RabbitMQ management (`rabbituser` / `rabbitpass`) |

---

## How It Works

### Appointment Booking Flow

1. Patient registers and logs in → receives a JWT
2. Browses doctors → picks a date → picks an available slot
3. `POST /api/appointments` → Spring Boot saves appointment with `status = PENDING` → publishes event to RabbitMQ
4. Python worker consumes the event → logs notification → updates `status = CONFIRMED`
5. Frontend polls `GET /api/appointments/{id}` every 2 seconds → displays live status flip

### Race Condition Handling

If a patient cancels an appointment **after** it was booked but **before** the worker processes the queue event, the worker detects `status = CANCELLED` in the DB and skips the confirmation. The check and update run in the same transaction.

---

## API Reference

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register patient |
| POST | `/api/auth/login` | Public | Login → JWT |
| GET | `/api/doctors` | JWT | List all doctors |
| GET | `/api/slots?doctorId=&date=` | JWT | Available time slots |
| POST | `/api/appointments` | JWT | Book appointment |
| DELETE | `/api/appointments/{id}` | JWT | Cancel appointment |
| GET | `/api/appointments` | JWT | All my appointments |
| GET | `/api/appointments/{id}` | JWT | Single + event log |

Full interactive docs at `/swagger-ui.html`.

---

## Running Tests

**Backend (44 tests):**
```bash
cd backend
mvn test
```

**Python worker (22 tests):**
```bash
docker exec -w /worker hap-worker python -m unittest tests.test_handler tests.test_db -v
```

---

## Environment Variables

See `.env.example` for all variables. Key ones:

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | `supersecretkey123changethisinprod` | **Change this in production** |
| `POSTGRES_PASSWORD` | `healthpass` | Change in production |
| `RABBITMQ_PASSWORD` | `rabbitpass` | Change in production |

---

## Project Structure

```
healthcare-appointment-platform/
├── backend/          Spring Boot REST API
├── worker/           Python notification worker
├── frontend/         React SPA + Nginx Dockerfile
├── docs/
│   ├── backend.md    Full backend documentation
│   ├── worker.md     Full worker documentation
│   └── frontend.md   Full frontend documentation
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Stopping the Platform

```bash
docker compose down          # stop containers, keep DB data
docker compose down -v       # stop containers + delete DB volume
```
