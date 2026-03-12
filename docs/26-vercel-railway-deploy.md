# Vercel + Railway Deployment Guide

## Purpose

This guide prepares MockStock Live for a split manual deployment:

- frontend on Vercel
- backend on Railway
- MySQL on Railway

Scope:

- no Docker
- no infrastructure automation
- no dashboard actions performed by Codex
- no local secret file changes

The goal is to make the manual dashboard flow predictable and low-risk.

---

## Architecture Split

### Frontend

- platform: Vercel
- directory: `frontend/`
- framework: React + Vite
- public URL example: `https://mockstock-live-frontend.vercel.app`

### Backend

- platform: Railway
- directory: repository root
- runtime: Spring Boot 4 / Java 21 / Gradle
- public URL example: `https://mockstock-live-api.up.railway.app`

### Database

- platform: Railway
- service type: MySQL
- backend connects through Railway environment variables

---

## What Is Ready In Code

- The frontend already uses `VITE_API_BASE_URL` as its backend base URL.
- The frontend no longer silently falls back to `http://localhost:8080` outside development. A deployed build now requires `VITE_API_BASE_URL`.
- The backend now uses `server.port=${PORT:8080}`, so Railway can inject its runtime port safely.
- The backend no longer hard-activates the `local` profile. Local still works through the default profile, while Railway can use a dedicated deployment profile.
- A new `deploy` profile is available through `src/main/resources/application-deploy.yml`.
- Mock stock seeding and mock quote generation now run in both `local` and `deploy`, so Railway still has seeded stocks and live quote updates.
- HTTP CORS and WebSocket/STOMP allowed origins now use the same environment-driven setting: `APP_CORS_ALLOWED_ORIGIN_PATTERNS`.

Performance implication:

- negligible runtime cost
- no business logic redesign
- no query-path change
- only startup/configuration behavior was adjusted for deployment safety

---

## Required Environment Variables

### Vercel

Required:

- `VITE_API_BASE_URL`

Optional:

- `VITE_WS_URL`
  Only set this if the WebSocket endpoint is not the default `wss://<backend-host>/ws` derived from `VITE_API_BASE_URL`.

Recommended production value example:

```bash
VITE_API_BASE_URL=https://mockstock-live-api.up.railway.app
```

Optional override example:

```bash
VITE_WS_URL=wss://mockstock-live-api.up.railway.app/ws
```

### Railway Backend Service

Required:

- `SPRING_PROFILES_ACTIVE=deploy`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`

Recommended value shape:

```bash
SPRING_PROFILES_ACTIVE=deploy
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://mockstock-live-frontend.vercel.app,https://mockstock-live-frontend-git-*.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

Notes:

- `MySQL` in `${{MySQL.MYSQLHOST}}` is the Railway MySQL service name example. If you rename the DB service, use that actual service name.
- `PORT` is provided by Railway automatically. Do not add it manually unless you are overriding platform behavior on purpose.
- `RAILWAY_PUBLIC_DOMAIN` is Railway-provided. The app does not require it as an input variable.

### Railway MySQL Service

No manual application variables are required inside the MySQL service itself if you use Railway's built-in MySQL template.

The backend will reference these Railway-provided MySQL variables:

- `MYSQLHOST`
- `MYSQLPORT`
- `MYSQLDATABASE`
- `MYSQLUSER`
- `MYSQLPASSWORD`

---

## Manual Steps In Dashboard Order

## 1. Railway: create the project and database first

1. Create a new Railway project.
2. Add a MySQL service from Railway's database options.
3. Wait until the MySQL service finishes provisioning.
4. Keep the MySQL service name simple, such as `MySQL`, because the backend variable references will use that name.

Expected result:

- one Railway project
- one MySQL service

## 2. Railway: add the backend service

1. In the same Railway project, add a new service from your GitHub repository.
2. Point Railway at this repository.
3. Set the service root directory to the repository root, not `frontend/`.
4. Let Railway detect the Java/Gradle app.

Expected result:

- one backend service connected to the repo root

## 3. Railway: add backend variables

Open the backend service `Variables` tab and add:

```bash
SPRING_PROFILES_ACTIVE=deploy
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:5173,http://127.0.0.1:5173
```

Why the temporary CORS value:

- Vercel does not exist yet at this point
- this lets the backend deploy first
- you will update `APP_CORS_ALLOWED_ORIGIN_PATTERNS` after Vercel gives you the real frontend URL

## 4. Railway: generate the public backend domain

1. Open the backend service `Settings` or `Networking` area.
2. Generate a Railway public domain.
3. Wait for the domain to become active.
4. Save the resulting URL.

Expected URL example:

- `https://mockstock-live-api.up.railway.app`

## 5. Railway: verify backend health before touching Vercel

Verify these URLs in the browser or with a client:

- `GET https://<railway-backend-domain>/actuator/health`
- `GET https://<railway-backend-domain>/api/v1/stocks`

Expected result:

- health endpoint returns `UP`
- stocks endpoint returns seeded mock stocks

## 6. Vercel: create the frontend project

1. Import the same GitHub repository into Vercel.
2. In project configuration, set `Root Directory` to `frontend`.
3. Confirm the framework is detected as `Vite`.
4. Confirm the build command is `npm run build`.
5. Confirm the output directory is `dist`.

## 7. Vercel: add frontend variables

In the Vercel project environment variables, add:

```bash
VITE_API_BASE_URL=https://<railway-backend-domain>
```

Set it for:

- Production
- Preview if you want preview deployments to call the shared Railway backend

Only add this if needed:

```bash
VITE_WS_URL=wss://<railway-backend-domain>/ws
```

Most deployments do not need `VITE_WS_URL` because the frontend derives it from `VITE_API_BASE_URL`.

## 8. Vercel: deploy the frontend

1. Trigger the first Vercel deployment.
2. Copy the generated production URL after the deploy succeeds.

Expected URL example:

- `https://mockstock-live-frontend.vercel.app`

## 9. Railway: update backend CORS with the real Vercel origin

Return to the Railway backend service and change:

```bash
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://<vercel-production-domain>,https://<vercel-project-name>-git-*.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

Then redeploy the backend.

Why this matters:

- HTTP API requests from Vercel need CORS
- SSE quote streaming also depends on browser CORS
- WebSocket/STOMP uses the same allowed-origin pattern list now

---

## Expected URLs After Deployment

- frontend: `https://<vercel-project>.vercel.app`
- backend: `https://<railway-service>.up.railway.app`
- backend health: `https://<railway-service>.up.railway.app/actuator/health`
- backend API example: `https://<railway-service>.up.railway.app/api/v1/stocks`
- backend SSE example: `https://<railway-service>.up.railway.app/api/v1/quotes/stream`
- backend WebSocket endpoint: `wss://<railway-service>.up.railway.app/ws`

---

## Post-Deploy Verification Checklist

### Backend

- `GET /actuator/health` returns `UP`
- `GET /api/v1/stocks` returns the seeded stock list
- `GET /actuator/prometheus` responds with text when requested with `Accept: text/plain`
- Railway logs show Flyway migrations completed successfully
- Railway logs do not show datasource connection failures

### Frontend

- the Vercel page loads without a blank screen
- the page shows the Railway backend base URL in the header
- signup works
- login works
- holdings and trade history load after login

### Real-time

- quote status changes to `connected`
- stock prices update without a full page refresh
- chat can connect through WebSocket
- join room works
- sending a chat message succeeds

### Cross-origin sanity checks

- browser devtools show no CORS errors for `/api/v1/**`
- SSE stream connects from the Vercel origin
- WebSocket handshake to `/ws` succeeds from the Vercel origin

---

## Limitations And Caveats

- This remains a portfolio deployment, not a hardened production environment.
- There is still no managed secret automation, staging environment policy, or infrastructure as code in this repository.
- The frontend is still a lightweight demo UI, not a full production frontend architecture.
- The backend still uses the in-app mock quote generator instead of an external market feed.
- Preview deployments on Vercel require matching CORS patterns if they should call the Railway backend.
- The backend service depends on Railway MySQL being healthy before Spring Boot can start successfully.
- There is no separate production seed toggle yet; the same mock stock seeding and quote generation used locally also run in the `deploy` profile because mock data is the intended product behavior of this portfolio app.

---

## Manual Work Still Required From You

### In Vercel

- import the repository
- set the root directory to `frontend`
- set `VITE_API_BASE_URL`
- deploy and copy the real frontend URL

### In Railway

- create the MySQL service
- create the backend service from GitHub
- set the backend variables
- generate the public backend domain
- update `APP_CORS_ALLOWED_ORIGIN_PATTERNS` after the Vercel URL exists

---

## Reference Docs

- Vercel build configuration and root directory settings:
  https://vercel.com/docs/deployments/configure-a-build
- Vercel monorepo root-directory workflow:
  https://vercel.com/docs/monorepos
- Railway Spring Boot deployment guide:
  https://docs.railway.com/guides/spring-boot
- Railway MySQL variables:
  https://docs.railway.com/guides/mysql
- Railway variable references:
  https://docs.railway.com/variables

