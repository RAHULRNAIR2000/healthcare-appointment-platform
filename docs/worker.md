# Python Notification Worker — Documentation

## Overview

The worker is a lightweight Python service that consumes appointment booking events from RabbitMQ, simulates sending a notification, and updates the appointment status from `PENDING` to `CONFIRMED` in PostgreSQL.

| Property | Value |
|---|---|
| Language | Python 3.11 |
| Libraries | pika 1.3.2, psycopg2-binary 2.9.9 |
| Messaging | RabbitMQ (AMQP) |
| Database | PostgreSQL (direct SQL via psycopg2) |

---

## Purpose

The worker exists to demonstrate and prove the event-driven pipeline. Spring Boot intentionally **never** sets an appointment to `CONFIRMED` — it only publishes an event. The worker is the only component allowed to confirm appointments. This makes the status change observable and proves the async message pipeline is working end to end.

---

## File Structure

```
worker/
├── Dockerfile
├── requirements.txt
├── app/
│   ├── __init__.py
│   ├── main.py       — entry point: RabbitMQ connection + consumer loop
│   ├── handler.py    — event processing logic
│   ├── db.py         — PostgreSQL queries
│   └── config.py     — environment variable loading
└── tests/
    ├── __init__.py
    ├── test_handler.py
    └── test_db.py
```

---

## Processing Flow

```
RabbitMQ delivers message (appointment.booked)
        │
        ▼
  on_message() callback fires
        │
        ▼
  handle_appointment_booked(body)
        │
        ├─ Parse JSON payload
        ├─ Log: "Processing appointment.booked | id=... | patient=... | doctor=..."
        │
        ▼
  confirm_appointment(appointment_id, patient_email, doctor_name)
        │
        ├─ SELECT status FROM appointments WHERE id = ?
        │
        ├─ If status != 'PENDING'  ──► return False  (skip, already cancelled/confirmed)
        │
        └─ If status == 'PENDING':
              INSERT appointment_logs (NOTIFICATION_SENT)
              UPDATE appointments SET status = 'CONFIRMED'
              INSERT appointment_logs (CONFIRMED)
              return True
        │
        ▼
  channel.basic_ack()   — remove message from queue
```

---

## RabbitMQ Contract

| Property | Value |
|---|---|
| Exchange | `appointment.events` (topic, durable) |
| Queue | `appointment.notification.queue` (durable) |
| Routing Key | `appointment.booked` |
| prefetch_count | 1 — process one message at a time |

**Consumed event payload:**
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

---

## Database Operations

The worker connects directly to PostgreSQL using psycopg2. It does not go through the Spring Boot API.

### Status check before confirming
```sql
SELECT status FROM appointments WHERE id = %s
```
This guard prevents the worker from overwriting a `CANCELLED` status. If the patient cancels between booking and the worker processing the event, the worker detects the `CANCELLED` status and skips processing.

### Insert notification log
```sql
INSERT INTO appointment_logs (appointment_id, event_type, message)
VALUES (%s, 'NOTIFICATION_SENT', %s)
```

### Update status to CONFIRMED
```sql
UPDATE appointments SET status = 'CONFIRMED', updated_at = NOW() WHERE id = %s
```

### Insert confirmed log
```sql
INSERT INTO appointment_logs (appointment_id, event_type, message)
VALUES (%s, 'CONFIRMED', %s)
```

All three writes happen within a **single transaction**. If any step fails, the entire transaction rolls back and the message is nacked (not requeued, to avoid poison-message loops).

---

## Error Handling

| Scenario | Behaviour |
|---|---|
| RabbitMQ not ready on startup | Retries up to 10 times with 3s delay between attempts |
| Message processing fails (DB error, parse error) | `basic_nack(requeue=False)` — message discarded, not retried |
| Appointment already CANCELLED when worker processes | Skips silently, logs a WARNING, ACKs the message |
| Appointment already CONFIRMED (duplicate delivery) | Skips silently, ACKs the message (idempotent) |

---

## Race Condition Handling

**Scenario:** Patient books → cancels → worker processes the old event.

Without the status check, the worker would overwrite `CANCELLED` → `CONFIRMED`. The guard query prevents this:

```python
current_status = get_appointment_status(appointment_id, cursor)
if current_status != 'PENDING':
    return False   # skip — don't write anything
```

The check and update are in the same database transaction, so there is no window for a race between checking and writing.

---

## Configuration

All config is loaded from environment variables in `app/config.py`:

| Variable | Default (local) | Description |
|---|---|---|
| `RABBITMQ_HOST` | localhost | RabbitMQ hostname |
| `RABBITMQ_PORT` | 5672 | RabbitMQ AMQP port |
| `RABBITMQ_USER` | rabbituser | |
| `RABBITMQ_PASSWORD` | rabbitpass | |
| `DB_HOST` | localhost | PostgreSQL hostname |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | healthcare | Database name |
| `DB_USER` | healthuser | |
| `DB_PASSWORD` | healthpass | |

---

## Running Locally

The worker runs as a Docker container. It starts automatically with docker compose.

```bash
# Start all services including worker
wsl docker compose up -d

# Watch worker logs live
wsl docker logs -f hap-worker

# Run tests (inside container)
wsl docker exec -w /worker hap-worker python -m unittest tests.test_handler tests.test_db -v
```

---

## Test Coverage

| File | Scenarios Covered | Tests |
|---|---|---|
| `test_handler.py` | Happy path, cancelled skip, field parsing, DB failure, invalid JSON, missing fields | 9 |
| `test_db.py` | Status check returns correct value, PENDING confirms with 3 writes, CANCELLED skips all writes, CONFIRMED idempotent skip, connection always closed | 13 |
| **Total** | | **22** |

All tests use `unittest.mock` to mock `psycopg2` connections and the `confirm_appointment` function. No real database or RabbitMQ connection is required to run tests.

---

## Dockerfile

```dockerfile
FROM python:3.11-slim
WORKDIR /worker
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app/ ./app/
CMD ["python", "-m", "app.main"]
```

---

## Assumptions

1. **No real email is sent.** Notification is simulated by logging to console and writing a `NOTIFICATION_SENT` row to `appointment_logs`. In production, this is where an email provider (SendGrid, AWS SES) would be called.
2. **Messages are not requeued on failure.** A failed message is nacked with `requeue=False` to avoid infinite poison-message loops. In production, a dead-letter queue should be configured.
3. **The worker processes one message at a time** (`prefetch_count=1`). This is intentional for simplicity. For higher throughput, multiple worker instances can be scaled horizontally — RabbitMQ distributes messages across consumers automatically.
4. **Worker connects directly to PostgreSQL**, bypassing the Spring Boot API. This is intentional — the worker is a backend service, not a client of the API. It has direct DB access via the shared Docker network.
5. **The worker is stateless.** It can be restarted at any time without data loss. Unprocessed messages remain in the durable queue until a worker picks them up.
