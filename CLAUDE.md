# Healthcare Appointment Platform

## Project Overview
A healthcare appointment management system built as a backend engineer take-home assignment.
Three services communicate via RabbitMQ: Spring Boot API, Python notification worker, Vite+React frontend.

## Dev Environment
- **Dev folder:** D:\healthcare-appointment-platform
- **GitHub:** https://github.com/RAHULRNAIR2000/healthcare-appointment-platform.git
- **NEVER use:** rahul-tarento GitHub account
- **Platform:** Windows 11, Docker running inside WSL
- **All docker commands must use:** `wsl docker ...` or `wsl --cd /mnt/d/healthcare-appointment-platform docker compose ...`
- **Project path in WSL:** `/mnt/d/healthcare-appointment-platform`

## Port Allocation (conflict-free with existing local projects)

| Service          | Host Port | Internal Docker Port |
|------------------|-----------|----------------------|
| PostgreSQL       | 5433      | 5432                 |
| RabbitMQ AMQP    | 5673      | 5672                 |
| RabbitMQ Mgmt UI | 15673     | 15672                |
| Spring Boot      | 9090      | 9090                 |
| Python Worker    | 8001      | 8001                 |
| Frontend (Vite)  | 5173      | 5173                 |

Existing projects that must NOT be disturbed: rable-admin (7075), rabble-mobile (8088), rable-mobile-pvs (8089).
Inside Docker network, containers talk to each other on INTERNAL ports (postgres:5432, rabbitmq:5672).

## Tech Stack
- **Backend:** Spring Boot 3.x, Java 17, Maven
- **Worker:** Python 3.11, pika, psycopg2-binary, python-dotenv
- **Database:** PostgreSQL 15 — migrations via Flyway
- **Messaging:** RabbitMQ 3.12
- **Auth:** JWT stateless (HS256, NO DB storage)
- **API Docs:** SpringDoc OpenAPI — Swagger UI at http://localhost:9090/swagger-ui.html
- **Frontend:** Vite + React
- **Containers:** Docker Compose

## Architecture — The Core Event-Driven Flow

```
Patient → POST /api/appointments
  → Spring Boot saves appointment (status = PENDING)
  → Spring Boot publishes event to RabbitMQ exchange: appointment.events
  → Python worker consumes event from queue: appointment.notification.queue
  → Worker logs notification + inserts appointment_logs row
  → Worker updates appointments.status = CONFIRMED
  → Frontend polls GET /api/appointments/{id} → sees PENDING → CONFIRMED
```

**CRITICAL RULE:** Spring Boot NEVER sets status=CONFIRMED. Only the Python worker does.
This is intentional — it proves the event-driven pipeline is genuinely working end-to-end.

## JWT Strategy — Stateless (Industry Standard)
- No token stored in database — EVER
- On login: server signs { userId, email, exp } with HS256 + JWT_SECRET env var → returns token
- On every request: client sends Authorization: Bearer <token>
- Server validates: checks signature + expiry only — zero DB lookup required
- Logout: client deletes token from localStorage

## Database Schema
Four tables: users, doctors, appointments, appointment_logs

Key rules:
- doctors are PRE-SEEDED via Flyway V2__seed_doctors.sql — NO doctor registration endpoint
- appointments.status values: PENDING → CONFIRMED (via worker) or CANCELLED (via API)
- UNIQUE constraint on (doctor_id, slot_start) prevents duplicate bookings
- Use @Lock(LockModeType.PESSIMISTIC_WRITE) on appointment creation for concurrency

## Spring Boot Package Structure
```
com.healthcare.appointment/
├── config/       SecurityConfig, RabbitMQConfig, SwaggerConfig
├── controller/   AuthController, DoctorController, AppointmentController
├── service/      AuthService, AppointmentService, SlotService
├── repository/   UserRepository, DoctorRepository, AppointmentRepository, AppointmentLogRepository
├── entity/       User, Doctor, Appointment, AppointmentLog
├── dto/          LoginRequest, RegisterRequest, AppointmentRequest, + Response DTOs
├── event/        AppointmentEventPublisher
├── security/     JwtUtil, JwtFilter
└── exception/    GlobalExceptionHandler (@ControllerAdvice)
```

## Python Worker Structure
```
worker/app/
├── main.py      RabbitMQ consumer setup + entry point
├── handler.py   Event handling logic (notification + DB update)
├── db.py        PostgreSQL connection + queries
└── config.py    Env var loading via python-dotenv
```

## RabbitMQ Contract
- Exchange name: `appointment.events` (type: topic)
- Routing key (Spring Boot publishes with): `appointment.booked`
- Queue name (Python worker binds to): `appointment.notification.queue`
- Binding: queue binds to exchange with routing key `appointment.booked`

Event payload:
```json
{
  "appointmentId": "uuid",
  "patientEmail": "john@example.com",
  "patientName": "John Doe",
  "doctorName": "Dr. Anjali Sharma",
  "hospitalName": "Apollo Hospital, Bangalore",
  "slotStart": "2026-06-28T10:00:00Z",
  "slotEnd": "2026-06-28T10:30:00Z"
}
```

## API Endpoints

| Method | Path                         | Auth   | Description                     |
|--------|------------------------------|--------|---------------------------------|
| POST   | /api/auth/register           | Public | Register patient                |
| POST   | /api/auth/login              | Public | Login → JWT                     |
| GET    | /api/doctors                 | JWT    | List all doctors                |
| GET    | /api/slots?doctorId=&date=   | JWT    | Available time slots            |
| POST   | /api/appointments            | JWT    | Book appointment                |
| DELETE | /api/appointments/{id}       | JWT    | Cancel own appointment          |
| GET    | /api/appointments            | JWT    | All my appointments             |
| GET    | /api/appointments/{id}       | JWT    | Single appointment + event log  |

## What NOT To Do
- Do NOT set appointment status=CONFIRMED in Spring Boot — only the Python worker does this
- Do NOT store JWT tokens in the database
- Do NOT expose JPA entities directly from controllers — always use DTOs
- Do NOT use ports 5432, 5672, 15672, 7075, 8088, 8089 — taken by other projects
- Do NOT push to rahul-tarento GitHub — only RAHULRNAIR2000
- Do NOT edit Flyway migrations that have already run — create a new one instead
- Do NOT add a doctor registration/login endpoint — doctors are seeded data only

## Running Locally
```bash
cd D:\healthcare-appointment-platform
docker compose up -d

# Logs
docker compose logs -f backend
docker compose logs -f worker

# Access
# Swagger UI:      http://localhost:9090/swagger-ui.html
# RabbitMQ Mgmt:   http://localhost:15673  (rabbituser / rabbitpass)
# PostgreSQL:      localhost:5433          (healthuser / healthpass / healthcare)
```

## Git Workflow
- Remote: https://github.com/RAHULRNAIR2000/healthcare-appointment-platform.git
- Branch: main
- Commit format: `feat:`, `fix:`, `chore:` followed by short description
