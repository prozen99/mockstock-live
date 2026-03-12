# Vercel + Railway Deployment Guide

## Purpose

This document now records the deployment flow that actually worked on `2026-03-12`, not just a theoretical preparation plan.

The project is successfully running in a split deployment:

- frontend on Vercel
- backend on Railway
- MySQL on Railway

Scope:

- manual dashboard deployment only
- no Docker
- no infrastructure automation
- no secret values committed into the repository

To keep the repository reusable, live project URLs are described as placeholders here even though the real Railway and Vercel URLs were verified successfully today.

---

## Verified Status On 2026-03-12

The following was verified against the deployed application today:

- Railway backend public URL responds
- Vercel frontend URL responds
- `GET /actuator/health` works on the deployed backend
- `GET /api/v1/stocks` works on the deployed backend
- signup and login work from the deployed frontend
- trading works from the deployed frontend
- holdings and trade history load from the deployed frontend
- SSE quote streaming works from the deployed frontend
- WebSocket/STOMP chat works from the deployed frontend

Important nuance:

- the backend root path `/` returned the standard JSON error response because no root route exists
- that did not mean the deployment was down
- the real deployment proof was the health endpoint and real API endpoints

---

## Final Deployment Architecture

### Frontend

- platform: Vercel
- directory: `frontend/`
- framework: React + Vite
- live URL shape: `https://<vercel-project>.vercel.app`

### Backend

- platform: Railway
- directory: repository root
- runtime: Spring Boot 4 / Java 21 / Gradle
- live URL shape: `https://<railway-service>.up.railway.app`

### Database

- platform: Railway
- service type: MySQL
- backend connects through Railway-provided MySQL variables referenced by backend env vars

---

## Exact Deployment Order That Worked

## 1. Railway first: identify the real project, service, and database

What actually happened:

- Railway showed confusing repository/service state because duplicate services and duplicate databases existed
- one service showed `There is no active deployment`
- another similarly named service held the real backend deployment

What worked:

1. open the Railway project and inspect every backend-like service
2. compare deployment history, logs, variables, and public domain assignment
3. identify the service that actually had recent successful build output
4. ignore or remove duplicate services only after confirming which one is real
5. do the same for duplicate MySQL services and confirm which database the real backend references

Why this mattered:

- service names alone were not reliable
- the correct backend and correct database had to be identified before any later env or domain work made sense

## 2. Railway backend service: stop relying on default build detection

What actually happened:

- Railway's default build behavior was not reliable enough for this repo
- the working deployment required explicit custom build and start commands

The build command that worked:

```bash
chmod +x ./gradlew && ./gradlew bootJar -x test --no-daemon
```

The start command that worked:

```bash
java -jar build/libs/mockstock-live-0.0.1-SNAPSHOT.jar
```

Why this mattered:

- the build had to produce the boot jar explicitly
- the service had to launch the generated jar explicitly

## 3. Railway backend service: set the verified env vars

The backend deployment that worked used these variables:

```bash
SPRING_PROFILES_ACTIVE=deploy
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://<vercel-production-domain>,https://<vercel-project-name>-git-*.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

Notes from today:

- `MySQL` above is the Railway DB service name example; it must match the real DB service name in the Railway project
- `PORT` was not entered manually; Railway supplied it and the app consumed it through `server.port=${PORT:8080}`

## 4. Railway backend verification: use health/API URLs, not `/`

What actually worked:

1. wait for Railway to mark the deployment successful
2. open the backend public domain
3. ignore the root-path JSON error at `/`
4. verify the backend with:

```text
GET https://<railway-backend-domain>/actuator/health
GET https://<railway-backend-domain>/api/v1/stocks
```

Expected proof:

- `/actuator/health` returns `UP`
- `/api/v1/stocks` returns seeded mock stocks

## 5. Vercel frontend deployment after backend verification

What worked in practice:

1. import the same repository into Vercel
2. set `Root Directory` to `frontend`
3. let Vercel build the Vite app
4. add the backend base URL as a Vercel env var

The Vercel env var that worked:

```bash
VITE_API_BASE_URL=https://<railway-backend-domain>
```

Important note from today:

- the value needed the full `https://` prefix
- entering only the hostname was not enough

Optional only if needed:

```bash
VITE_WS_URL=wss://<railway-backend-domain>/ws
```

This was not required when the WebSocket endpoint was the normal `/ws` on the same Railway host.

## 6. Final CORS correction after Vercel existed

What actually mattered today:

- Railway backend CORS had to include the deployed Vercel frontend origin
- preview-style Vercel domains were also worth allowing
- local Vite origins still needed to stay in the list for local verification

The working CORS variable shape was:

```bash
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://<vercel-production-domain>,https://<vercel-project-name>-git-*.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

Why this variable was needed:

- HTTP API requests from Vercel need browser CORS approval
- SSE quote streaming uses the same browser-origin rules
- WebSocket/STOMP handshake now uses the same configured origin patterns

## 7. Final end-to-end verification flow

After both platforms were live and CORS was corrected, the following flow worked:

1. open the Vercel frontend
2. confirm the frontend shows the backend base URL
3. sign up or log in
4. load stocks
5. execute a buy or sell
6. confirm holdings refresh
7. confirm trade history refresh
8. confirm quote status becomes connected and quote values update
9. connect chat
10. join a room
11. send a message and confirm it appears

---

## Exact Environment Variables Used

### Vercel

Required and verified:

- `VITE_API_BASE_URL=https://<railway-backend-domain>`

Optional:

- `VITE_WS_URL=wss://<railway-backend-domain>/ws`

### Railway Backend

Required and verified:

- `SPRING_PROFILES_ACTIVE=deploy`
- `SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8`
- `SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}`
- `SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://<vercel-production-domain>,https://<vercel-project-name>-git-*.vercel.app,http://localhost:5173,http://127.0.0.1:5173`

### Railway MySQL

Railway supplied the DB service variables that the backend referenced:

- `MYSQLHOST`
- `MYSQLPORT`
- `MYSQLDATABASE`
- `MYSQLUSER`
- `MYSQLPASSWORD`

---

## What Was Verified Today Vs. What Remains Theoretical

Verified today:

- split deployment on Vercel + Railway + Railway MySQL
- explicit Railway custom build command
- explicit Railway custom start command
- Railway backend public domain
- Vercel frontend public domain
- Vercel `VITE_API_BASE_URL` using full `https://...`
- Railway CORS variable including deployed Vercel domains and local origins
- frontend API, trading, holdings, SSE, and chat behavior after deployment

Still out of scope:

- infrastructure automation
- custom domain setup
- staging/production environment separation
- deployment rollback automation

---

## Post-Deploy Verification Checklist

### Backend

- `GET /actuator/health` returns `UP`
- `GET /api/v1/stocks` returns stock data
- Railway logs show the boot jar started successfully
- Railway logs do not show datasource failures
- Railway logs show Flyway migration success or no-op up-to-date migration status

### Frontend

- Vercel page loads successfully
- frontend displays the backend base URL
- signup and login work
- trade actions succeed
- holdings and trade history load

### Real-time

- quote stream connects
- quotes update on screen
- chat WebSocket connects
- room join works
- message send works

### Cross-origin

- no browser CORS errors for API calls
- SSE works from the Vercel origin
- WebSocket handshake works from the Vercel origin

---

## Deployment Lessons Learned

- In Railway, confirm the real active backend service before changing variables. Duplicate services can make the UI misleading.
- If Railway says `There is no active deployment`, verify whether that message belongs to the wrong duplicate service before debugging the application.
- Do not trust the backend root path `/` as a health signal in this app. Use `/actuator/health` and real API endpoints.
- For this repository, explicit Railway build/start commands were safer than default build detection.
- On Linux-based build environments like Railway, `gradlew` may need executable permission before the build runs.
- Vercel frontend env values must be complete URLs, including `https://`.
- CORS needs both deployed Vercel origins and local dev origins if you want both deployed use and local review flow to keep working.

---

## Reference Docs

- [README.md](../README.md)
- [docs/03-api-spec.md](./03-api-spec.md)
- [docs/13-troubleshooting.md](./13-troubleshooting.md)
- [docs/25-frontend-demo.md](./25-frontend-demo.md)
