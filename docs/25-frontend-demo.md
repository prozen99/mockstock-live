# Frontend Demo

## Purpose

Phase 11 adds a minimal local frontend demo for the backend that already exists. It is not a product UI and it is not a full frontend architecture exercise. The goal is to let a reviewer verify the implemented backend flows visually with as little extra complexity as possible.

## Frontend Structure

The frontend lives under [`frontend/`](../frontend/).

Key files:

- [`package.json`](../frontend/package.json)
  Vite + React scripts and the one extra runtime library, `@stomp/stompjs`
- [`src/App.jsx`](../frontend/src/App.jsx)
  The single-page demo shell and explicit state management
- [`src/api.js`](../frontend/src/api.js)
  Small fetch helpers, API response unwrapping, SSE setup, and WebSocket URL derivation
- [`src/styles.css`](../frontend/src/styles.css)
  Minimal clean styling for local verification
- [`.env.example`](../frontend/.env.example)
  Local backend base URL example

## What The Demo Shows

- stock list
- stock detail panel
- demo user signup/login for selecting an existing backend user
- buy and sell actions against the existing trade APIs
- holdings view
- trade history view
- live quote updates through SSE
- room list, message list, join, WebSocket connect/disconnect, and chat send through STOMP

## Backend Integration Points

HTTP APIs used:

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{stockId}`
- `POST /api/v1/trades/buy`
- `POST /api/v1/trades/sell`
- `GET /api/v1/trades/history`
- `GET /api/v1/portfolio/holdings`
- `GET /api/v1/chat/rooms`
- `GET /api/v1/chat/rooms/{roomId}/messages`
- `POST /api/v1/chat/rooms/{roomId}/join`

Realtime APIs used:

- `GET /api/v1/quotes/stream`
- WebSocket endpoint `/ws`
- STOMP subscribe `/sub/chat/rooms/{roomId}`
- STOMP send `/pub/chat/rooms/{roomId}`

## Minimal Compatibility Adjustment

One backend compatibility change was added for the Phase 11 demo:

- local browser CORS support for `http://localhost:5173` and `http://127.0.0.1:5173` on `/api/v1/**`

Why:
Vite runs on a separate local origin, and the demo calls the existing backend directly for HTTP and SSE.

Scope:
This does not change business logic or API contracts. It only allows local browser access for the demo layer.

Performance implication:
Negligible. It adds standard CORS header handling for a small set of local origins.

## How To Run Locally

### 1. Start the backend

If Java is not configured in the current shell:

```powershell
$env:JAVA_HOME='C:\Users\admin\.jdks\ms-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Then run:

```powershell
.\gradlew.bat bootRun
```

If `8080` is already in use:

```powershell
.\gradlew.bat bootRun --args=--server.port=8081
```

### 2. Configure the frontend backend URL

From [`frontend/`](../frontend/), create `.env.local` if needed:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

If the backend is running on `8081`, change the value accordingly.

### 3. Install frontend dependencies

```bash
npm install
```

### 4. Start the Vite demo

```bash
npm run dev
```

Open the URL shown by Vite, typically `http://localhost:5173`.

## Suggested Local Demo Flow

1. Sign up a demo user or log in with an existing one.
2. Select a stock from the list and watch live price updates.
3. Execute a buy or sell.
4. Confirm holdings and trade history refresh.
5. Open the chat panel, connect WebSocket, join a room, and send a message.

## What Is Intentionally Simplified

- single-page layout instead of a routed product UI
- simple component/state structure instead of a larger frontend architecture
- no frontend auth/session persistence beyond in-memory selected user state
- no charting layer, unread-state UX, or message history infinite scroll
- trade history uses the existing offset endpoint for a compact demo view
- browser-level verification only; no frontend test harness in this phase

## Limitations

- The frontend is designed for local demo use, not deployment.
- It assumes the backend is already running and reachable.
- It does not attempt to replace backend docs or existing HTML test artifacts.
- It does not surface every backend metric or every backend endpoint.
- It keeps one active chat room subscription at a time for clarity.

## Interview-Ready Explanation

The frontend was kept intentionally small because the value of this project is still the backend. The demo exists to make the backend flows easier to verify visually, not to compete with the backend as a separate architecture project.

That choice is intentional and defensible:

- React + Vite is enough to demonstrate the APIs quickly
- SSE and STOMP behavior become easier for a reviewer to see live
- the code stays reviewable because state is explicit and local
- the phase avoids turning into a design-system, deployment, or frontend-framework exercise
