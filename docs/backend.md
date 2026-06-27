# Backend Service — Documentation

## Overview

The backend is a Spring Boot REST API that handles patient authentication, doctor discovery, appointment booking, and event publishing. It is the core of the healthcare appointment management platform.

| Property | Value |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Build Tool | Maven |
| Port | 9090 |
| API Docs | http://localhost:9090/swagger-ui.html |

---

## Architecture

```
Client (React Frontend)
        │
        ▼ HTTP (JWT Bearer token)
┌──────────────────────────┐
│   Spring Boot REST API   │
│                          │
│  Controllers             │
│      │                   │
│  Services                │
│      │          │        │
│  Repositories   EventPub │
└──────┼──────────┼────────┘
       │          │
       ▼          ▼
  PostgreSQL   RabbitMQ
```

---

## Package Structure

```
com.healthcare.appointment/
├── config/
│   ├── SecurityConfig.java       — Spring Security + CORS + JWT filter wiring
│   ├── RabbitMQConfig.java       — Exchange, queue, binding, message converter
│   └── SwaggerConfig.java        — OpenAPI bearer auth definition
├── controller/
│   ├── AuthController.java       — POST /api/auth/register, /api/auth/login
│   ├── DoctorController.java     — GET /api/doctors, /api/slots
│   └── AppointmentController.java— POST/DELETE/GET /api/appointments
├── service/
│   ├── AuthService.java          — Registration, login, JWT generation
│   ├── AppointmentService.java   — Booking, cancellation, retrieval
│   └── SlotService.java          — Available slot calculation
├── repository/
│   ├── UserRepository.java
│   ├── DoctorRepository.java
│   ├── AppointmentRepository.java— Custom JPQL with pessimistic locking
│   └── AppointmentLogRepository.java
├── entity/
│   ├── User.java
│   ├── Doctor.java
│   ├── Appointment.java
│   └── AppointmentLog.java
├── dto/                          — Request/Response DTOs (entities never exposed directly)
├── event/
│   ├── AppointmentBookedEvent.java
│   └── AppointmentEventPublisher.java
├── security/
│   ├── JwtUtil.java              — Token generation and validation
│   └── JwtFilter.java            — OncePerRequestFilter, sets SecurityContext
└── exception/
    └── GlobalExceptionHandler.java — @RestControllerAdvice
```

---

## Authentication

### Strategy
Stateless JWT — no session, no token stored in the database.

### Flow
1. Patient registers or logs in → server signs a JWT with `{ userId, email, exp }` using HS256 + secret
2. Client stores token in `localStorage`
3. Every subsequent request sends `Authorization: Bearer <token>`
4. `JwtFilter` validates signature and expiry on every request, loads user from DB, sets `SecurityContext`
5. `@AuthenticationPrincipal User patient` in controllers resolves the current user

### Configuration
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}        # injected via environment variable
    expiration-ms: 86400000      # 24 hours
```

### Public endpoints (no token required)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `/swagger-ui/**`, `/api-docs/**`

---

## API Reference

### Auth

#### POST /api/auth/register
Register a new patient account.

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secret123"
}
```
**Response: 200**
```json
{
  "token": "eyJhbGci...",
  "name": "John Doe",
  "email": "john@example.com"
}
```
**Errors:** `400` — email already registered, validation failure

---

#### POST /api/auth/login
**Request:**
```json
{ "email": "john@example.com", "password": "secret123" }
```
**Response: 200** — same shape as register  
**Errors:** `400` — invalid credentials

---

### Doctors

#### GET /api/doctors
Returns all seeded doctors.

**Response: 200**
```json
[
  {
    "id": "uuid",
    "name": "Dr. Anjali Sharma",
    "hospitalName": "Apollo Hospital, Bangalore",
    "specialization": "Cardiology",
    "availableFrom": "09:00",
    "availableTo": "17:00",
    "slotDurationMinutes": 30
  }
]
```

---

#### GET /api/slots?doctorId={uuid}&date={YYYY-MM-DD}
Returns available (non-booked) time slots for a doctor on a given date.

**Response: 200**
```json
[
  { "slotStart": "2026-07-01T09:00:00Z", "slotEnd": "2026-07-01T09:30:00Z" },
  { "slotStart": "2026-07-01T09:30:00Z", "slotEnd": "2026-07-01T10:00:00Z" }
]
```
Slots already booked (status ≠ CANCELLED) are excluded from the response.

---

### Appointments

#### POST /api/appointments _(requires JWT)_
Book an appointment.

**Request:**
```json
{
  "doctorId": "uuid",
  "slotStart": "2026-07-01T09:00:00Z",
  "notes": "Regular checkup"
}
```
**Response: 201**
```json
{
  "id": "uuid",
  "doctorName": "Dr. Anjali Sharma",
  "hospitalName": "Apollo Hospital, Bangalore",
  "specialization": "Cardiology",
  "slotStart": "2026-07-01T09:00:00Z",
  "slotEnd": "2026-07-01T09:30:00Z",
  "status": "PENDING",
  "notes": "Regular checkup",
  "createdAt": "2026-06-27T10:00:00Z",
  "logs": [{ "eventType": "BOOKED", "message": "...", "createdAt": "..." }]
}
```
**Errors:** `409` — slot already booked, `404` — doctor not found

---

#### DELETE /api/appointments/{id} _(requires JWT)_
Cancel an appointment. Only the patient who booked it can cancel it.

**Response: 200** — updated appointment with `status: CANCELLED`  
**Errors:** `400` — not your appointment, `409` — already cancelled, `404` — not found

---

#### GET /api/appointments _(requires JWT)_
Returns all appointments for the authenticated patient, ordered by slot time descending.

---

#### GET /api/appointments/{id} _(requires JWT)_
Returns a single appointment with the full event log.

**Response: 200** — same shape as POST response, with populated `logs` array  
**Errors:** `400` — not your appointment, `404` — not found

---

## Database Schema

### users
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK, gen_random_uuid() |
| email | VARCHAR | UNIQUE NOT NULL |
| password_hash | VARCHAR | BCrypt hashed |
| name | VARCHAR | NOT NULL |
| created_at | TIMESTAMPTZ | auto-set on insert |

### doctors
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| name | VARCHAR | NOT NULL |
| hospital_name | VARCHAR | NOT NULL |
| specialization | VARCHAR | NOT NULL |
| available_from | TIME | working hours start |
| available_to | TIME | working hours end |
| slot_duration_minutes | INT | default 30 |
| created_at | TIMESTAMPTZ | |

### appointments
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| patient_id | UUID | FK → users |
| doctor_id | UUID | FK → doctors |
| slot_start | TIMESTAMPTZ | NOT NULL |
| slot_end | TIMESTAMPTZ | NOT NULL |
| status | VARCHAR | PENDING / CONFIRMED / CANCELLED |
| notes | TEXT | optional |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

**Unique constraint:** `(doctor_id, slot_start)` — database-level duplicate booking prevention.

### appointment_logs
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| appointment_id | UUID | FK → appointments |
| event_type | VARCHAR | BOOKED / NOTIFICATION_SENT / CONFIRMED / CANCELLED |
| message | TEXT | human-readable description |
| created_at | TIMESTAMPTZ | |

---

## RabbitMQ Contract

| Property | Value |
|---|---|
| Exchange | `appointment.events` (topic, durable) |
| Queue | `appointment.notification.queue` (durable) |
| Routing Key | `appointment.booked` |
| Message Format | JSON |

**Published event payload:**
```json
{
  "appointmentId": "uuid",
  "patientEmail": "john@example.com",
  "patientName": "John Doe",
  "doctorName": "Dr. Anjali Sharma",
  "hospitalName": "Apollo Hospital, Bangalore",
  "slotStart": "2026-07-01T09:00:00Z",
  "slotEnd": "2026-07-01T09:30:00Z"
}
```

**Important:** Spring Boot only publishes the `appointment.booked` event. It **never** sets status to `CONFIRMED` — that is exclusively the Python worker's responsibility.

---

## Concurrency Handling

Appointment booking uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the query that checks slot availability. This prevents two concurrent requests from booking the same slot simultaneously. The database-level `UNIQUE(doctor_id, slot_start)` constraint is a second layer of defence.

---

## Database Migrations

Managed by Flyway. Migration files are in `src/main/resources/db/migration/`:

| File | Description |
|---|---|
| `V1__init_schema.sql` | Creates all four tables |
| `V2__seed_doctors.sql` | Seeds 5 doctors |

**Rule:** Never modify a migration that has already run. Add a new `V3__...sql` file for any schema changes.

---

## Error Handling

All errors are handled by `GlobalExceptionHandler` and return a consistent JSON shape:
```json
{ "error": "Human readable message" }
```

| Exception | HTTP Status |
|---|---|
| `MethodArgumentNotValidException` | 400 — with field-level messages |
| `IllegalArgumentException` | 400 |
| `IllegalStateException` | 409 Conflict |
| `ResourceNotFoundException` | 404 |

---

## Configuration (application.yml)

```yaml
server:
  port: 9090

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/healthcare}
    username: ${SPRING_DATASOURCE_USERNAME:healthuser}
    password: ${SPRING_DATASOURCE_PASSWORD:healthpass}
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: ${SPRING_RABBITMQ_PORT:5673}
    username: ${SPRING_RABBITMQ_USERNAME:rabbituser}
    password: ${SPRING_RABBITMQ_PASSWORD:rabbitpass}
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: ${JWT_SECRET:supersecretkey123changethisinprod}
    expiration-ms: 86400000
```

All sensitive values are injected via environment variables. Defaults are for local development only and must not be used in production.

---

## Running Locally

```bash
# From project root
wsl docker compose up -d

# Verify backend is healthy
curl http://localhost:9090/swagger-ui.html

# Run tests
cd backend
mvn test
```

---

## Test Coverage

| Class | Type | Tests |
|---|---|---|
| `AuthServiceTest` | Unit | 5 |
| `AppointmentServiceTest` | Unit | 11 |
| `SlotServiceTest` | Unit | 5 |
| `AuthControllerTest` | Web layer | 7 |
| `DoctorControllerTest` | Web layer | 5 |
| `AppointmentControllerTest` | Web layer | 11 |
| **Total** | | **44** |

Controller tests use `@WebMvcTest` with `MockMvc`. Security config is imported via `@Import({SecurityConfig.class, JwtFilter.class})` to test the real JWT filter behaviour. Service tests use `@ExtendWith(MockitoExtension.class)` — no Spring context loaded.

---

## Assumptions

1. **Doctors are passive entities.** They do not register, log in, or interact with the system. They are pre-seeded via Flyway migration and cannot be added through the API.
2. **Only patients authenticate.** There is a single user role. No admin or doctor roles exist.
3. **Slot times are stored and returned in UTC.** The frontend is responsible for display conversion.
4. **A cancelled appointment's slot becomes available again.** The slot availability query excludes only non-cancelled appointments.
5. **Status transitions are one-directional.** `PENDING → CONFIRMED` (worker only), `PENDING/CONFIRMED → CANCELLED` (patient only). There is no re-open or reschedule flow.
6. **JWT secret must be at least 32 characters** for HS256 to be secure. The default dev value must be replaced in production.
7. **No email notifications are sent.** The Python worker simulates notification by writing to `appointment_logs`.
