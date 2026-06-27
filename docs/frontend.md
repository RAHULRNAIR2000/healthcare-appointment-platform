# Frontend — Documentation

## Overview

The frontend is a single-page application (SPA) built with Vite + React + TypeScript. It provides a patient-facing UI for registering, logging in, browsing doctors, booking appointments, and tracking appointment status in real time.

| Property | Value |
|---|---|
| Framework | React 18 + TypeScript |
| Build Tool | Vite 5 |
| Styling | Tailwind CSS v3 |
| HTTP Client | Axios |
| Routing | React Router v6 |
| Port | 5173 (dev) |

---

## File Structure

```
frontend/src/
├── api/
│   └── client.ts             — Axios instance with JWT interceptor + 401 handler
├── hooks/
│   └── useAuth.ts            — localStorage helpers: saveAuth, getToken, clearAuth
├── components/
│   ├── Navbar.tsx            — Top navigation bar with logout
│   ├── ProtectedRoute.tsx    — Redirects to /login if no token
│   └── StatusBadge.tsx       — Coloured pill: PENDING / CONFIRMED / CANCELLED
├── pages/
│   ├── LoginPage.tsx         — Login form
│   ├── RegisterPage.tsx      — Registration form
│   ├── DashboardPage.tsx     — Appointment list with stats banner
│   ├── BookPage.tsx          — 3-step booking flow
│   └── AppointmentDetailPage.tsx — Detail view with live status polling
└── App.tsx                   — Route definitions
```

---

## Routing

| Path | Access | Component | Description |
|---|---|---|---|
| `/login` | Public | `LoginPage` | Redirects to `/dashboard` if already logged in |
| `/register` | Public | `RegisterPage` | Redirects to `/dashboard` if already logged in |
| `/dashboard` | Protected | `DashboardPage` | Lists all appointments with stats |
| `/book` | Protected | `BookPage` | 3-step appointment booking flow |
| `/appointments/:id` | Protected | `AppointmentDetailPage` | Appointment detail + event log + live polling |
| `*` (catch-all) | — | Redirect | Goes to `/dashboard` or `/login` based on auth state |

Protected routes use `ProtectedRoute` which reads the token from localStorage. If absent, the user is redirected to `/login` before the page renders.

---

## Authentication Flow

```
Register/Login form
      │
      ▼
POST /api/auth/register  or  /api/auth/login
      │
      ▼
{ token, name, email }
      │
      ▼
saveAuth() → localStorage.setItem('token', ...)
             localStorage.setItem('user', JSON.stringify({name, email}))
      │
      ▼
navigate('/dashboard')
```

**Token lifecycle:**
- Stored in `localStorage` under key `token`
- Attached to every request via Axios request interceptor: `Authorization: Bearer <token>`
- On any `401` response from the API, the Axios response interceptor clears localStorage and redirects to `/login`
- On logout click, `clearAuth()` removes both `token` and `user` keys

---

## API Client

`src/api/client.ts` creates a single Axios instance used everywhere:

```typescript
const api = axios.create({ baseURL: 'http://localhost:9090' })

// Request interceptor — attach JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Response interceptor — handle expired/invalid token
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)
```

---

## Pages

### LoginPage / RegisterPage

- Centered card on soft gradient background
- Form validation is handled by the API (Spring `@Valid`) — errors displayed inline
- On success, token + user info saved to localStorage, redirected to dashboard

---

### DashboardPage

- **Hero banner** — dark navy gradient card with welcome message and 3 stat boxes (Confirmed, Pending, Cancelled counts)
- **Appointment grid** — 2-column card layout, each card has:
  - Specialization icon + doctor name + status badge
  - Date block (weekday, day, month) + time range
  - Hospital name
  - Optional notes
  - Colour-coded left border (green = CONFIRMED, yellow = PENDING, grey = CANCELLED)
  - "View Details" and "Cancel" buttons
- Cancel triggers a `DELETE /api/appointments/{id}` call then refreshes the list

---

### BookPage — 3-Step Flow

**Step 1 — Pick Doctor**
- Fetches `GET /api/doctors` on mount
- Doctor cards with specialization, hospital, working hours
- Selected doctor is highlighted with a navy border

**Step 2 — Pick Date**
- Custom calendar component (no external library)
- Navigates forward/backward by month
- Past dates are disabled and greyed out
- Selected date highlighted in navy

**Step 3 — Pick Slot**
- Fetches `GET /api/slots?doctorId=&date=` when step 3 loads
- Time slot buttons in a 2-column scrollable grid
- Selected slot highlighted in navy
- Confirm panel appears on the right when a slot is selected
- Optional notes input
- On confirm → `POST /api/appointments` → redirected to `/appointments/{id}`

---

### AppointmentDetailPage

- Displays doctor info, status badge, date/time, notes
- **Live status polling** — when status is `PENDING`, `GET /api/appointments/{id}` is called every 2 seconds
- Polling stops automatically when status changes to `CONFIRMED` or `CANCELLED`
- Visual indicators:
  - PENDING: pulsing yellow dot with "Waiting for confirmation..." message
  - CONFIRMED: green banner "Your appointment is confirmed!"
- **Event log timeline** — ordered list of all `appointment_logs` entries with icons:
  - 📋 BOOKED
  - 🔔 NOTIFICATION_SENT
  - ✅ CONFIRMED
  - ❌ CANCELLED
- Cancel button available for non-cancelled appointments

---

## Time Display

All times from the API are in UTC (ISO 8601 format, e.g. `2026-07-01T09:00:00Z`).

**Important:** `new Date(iso).toLocaleTimeString()` converts UTC to the browser's local timezone, which would show wrong times for users in IST (+5:30). All time formatting reads the hours and minutes directly from the ISO string to avoid timezone conversion:

```typescript
function formatTime(iso: string) {
  const timePart = iso.split('T')[1]         // "09:00:00Z"
  const [h, m] = timePart.split(':').map(Number)
  const ampm = h >= 12 ? 'PM' : 'AM'
  const h12 = h % 12 || 12
  return `${h12}:${String(m).padStart(2, '0')} ${ampm}`
}
```

---

## Design System

| Token | Value | Usage |
|---|---|---|
| `navy-700` | `#1e2a4a` | Primary buttons, headings, active states |
| `navy-800` | `#172040` | Button hover state |
| Background | `linear-gradient(135deg, #fdf4ff, #f0f4ff, #fef0f8)` | Full page background |
| Font | Inter | All text |
| Border radius | `rounded-2xl` / `rounded-3xl` | Cards and containers |
| Status: CONFIRMED | green-100 / green-700 | Badge, card border |
| Status: PENDING | yellow-100 / yellow-700 | Badge, card border |
| Status: CANCELLED | gray-100 / gray-500 | Badge, card border |

---

## CORS

The frontend (`localhost:5173`) makes cross-origin requests to the backend (`localhost:9090`). CORS is configured on the Spring Boot side in `SecurityConfig.java`:

```java
config.setAllowedOrigins(List.of("http://localhost:5173"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

No CORS configuration is needed on the frontend side.

---

## Running Locally

```bash
cd frontend
npm install
npm run dev
```

App available at `http://localhost:5173`. The backend must be running at `http://localhost:9090`.

---

## Build for Production

```bash
npm run build
```

Output is in `dist/`. In production, this is served by an Nginx container defined in `frontend/Dockerfile`.

---

## Assumptions

1. **No state management library** (no Redux, Zustand, etc.). All state is local to each page component using `useState`. The app is small enough that this is sufficient.
2. **No token expiry handling on the client side.** The 401 interceptor handles expired tokens server-side. The client does not decode the JWT or check expiry locally.
3. **Live polling uses `setInterval` at 2-second intervals** on the detail page. This is intentional for simplicity. A production system would use WebSockets or Server-Sent Events for push-based updates.
4. **The calendar does not restrict dates beyond "not in the past."** There is no logic to disable dates where all slots are already booked — the user picks a date and sees an empty slot list if nothing is available.
5. **No pagination on the dashboard.** All appointments are loaded in a single API call. This is acceptable for a demo but would need pagination for production.
6. **JWT is stored in localStorage.** This is a common pattern for SPAs but is vulnerable to XSS. For a production app, `httpOnly` cookies should be considered. For this assignment, localStorage is acceptable.
