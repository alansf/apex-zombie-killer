## Apex Zombie Killer - Heroku AppLink + Spring Boot Offload

This repo ships a demo that offloads long-running Apex batch workloads to a Heroku-hosted Spring Boot service, surfaced in Salesforce via Heroku AppLink and invoked via External Services or Named Credentials. It also includes a reusable UI utility to convert APEX code to Java or JavaScript (server-side API stubbed; wire your model key to enable LLM-based transformation).

### Components
- `server/`: Spring Boot app exposing:
  - Five job endpoints mirroring Apex batches
  - Code transform endpoints: APEX→Java and APEX→JavaScript
  - Static `openapi.yaml` for External Services and Agentforce tools
- `web/`: Vite + React front-end with Monaco-like editor UX (lightweight text area) for APEX conversion
- `heroku/connect-mapping.json`: Starter Heroku Connect mapping
- `scripts/`: Example Salesforce seed data and helpers

---

## Quick Start

```bash
cd /Users/alan.scott/Development/demo/apex-zombie-killer
```

### Salesforce (DevHub / Scratch Org)
```bash
sf org login web --set-default-dev-hub --alias DevHub
sf org create scratch --definition-file config/project-scratch-def.json \
  --set-default --duration-days 7 --alias apex-perf-demo

# (Optional) Deploy Named Credential metadata if you add it under force-app/
sf project deploy start --source-dir force-app --target-org apex-perf-demo

# Seed data for baselines (optional)
sf apex run --file scripts/seed_data.apex --target-org apex-perf-demo
```

### Heroku (App, Postgres, Connect, Deploy)
```bash
heroku login
heroku apps:create apex-perf-demo-$USER
heroku stack:set heroku-22 -a apex-perf-demo-$USER
heroku buildpacks:add heroku/java -a apex-perf-demo-$USER
heroku addons:create heroku-postgresql:standard-0 -a apex-perf-demo-$USER
heroku plugins:install heroku-connect
heroku addons:create herokuconnect:demo -a apex-perf-demo-$USER

# Import starter mapping
heroku connect:import heroku/connect-mapping.json -a apex-perf-demo-$USER

# Build and deploy server
cd server
./mvnw -q -DskipTests package || mvn -q -DskipTests package
heroku git:remote -a apex-perf-demo-$USER
git add . && git commit -m "server: initial" || true
git push heroku main
```

### Heroku AppLink (Salesforce UI)
- Salesforce Setup → Heroku → Link Heroku App → select `apex-perf-demo-$USER`.
- Add the AppLink surface to your Salesforce app (utility bar or app launcher).
- AppLink is for SSO/UX; use Named Credential or External Services for API callouts.

### External Services / Named Credential
- Serve OpenAPI at: `https://apex-perf-demo-$USER.herokuapp.com/openapi.yaml`
- Setup → External Services → Add Service → paste URL above → choose Named Credential.
- Or call via Apex using a Named Credential endpoint `callout:HerokuJobs/...`.

### Web UI (local dev)
```bash
cd /Users/alan.scott/Development/demo/apex-zombie-killer/web
npm install
npm run dev
```
The production UI is built and served by Spring Boot when you run:
```bash
cd /Users/alan.scott/Development/demo/apex-zombie-killer/web
npm run build
cp -R dist/* ../server/src/main/resources/static/
```

---

## Endpoints (server)
- POST `/jobs/product-purchase/run`
- POST `/jobs/revenue-file-import/run`
- POST `/jobs/account-plan-reporting/run`
- POST `/jobs/cpq-quote-oppty-sync/run`
- POST `/jobs/opportunity-split/run`
- POST `/transform/apex-to-java`
- POST `/transform/apex-to-js`
- GET  `/openapi.yaml`

All job endpoints accept:
```json
{ "batchSize": 1000, "maxConcurrency": 4, "dateRange": { "from": "2024-01-01", "to": "2025-12-31" }, "dryRun": false }
```

---

## Makefile
Key targets:
- `make heroku-deploy` – build and push server to Heroku
- `make ui-build` – build React UI and copy to Spring Boot static
- `make connect-import` – import Heroku Connect mappings

---

## Notes
- The transform API is stubbed to return the input with light scaffolding. Set `ANTHROPIC_API_KEY` and implement model wiring in `TransformService` to enable real conversions.
- For production, secure CORS/headers and replace demo-wide open CORS.


