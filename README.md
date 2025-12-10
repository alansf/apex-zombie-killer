# Apex Zombie Killer – Single‑App Multi‑Modal Runtime (Postgres‑backed)

Demo‑ready Spring Boot app that:
- Transforms Apex → Java/JS using Heroku Managed Inference
- Approves code, binds it to runtime endpoints, and publishes a dynamic OpenAPI for Salesforce External Services
- Executes code via web, queue, or simulated Postgres trigger paths

## Quick Start (Local)
```bash
# Java 21+ and Maven required
mvn -f server/pom.xml -DskipTests package
java -jar server/target/app.jar
```

Open `http://localhost:8080/` and try a transform + Approve.

## Heroku Deploy
Assumes you already have an app (e.g., apex-zombie-killer) and Heroku Postgres attached.

```bash
APP=apex-zombie-killer

# Build and deploy
git add .
git commit -m "runtime: postgres bindings/queue; inference streaming; UI improvements"
git push heroku main

# Required config
heroku config:set \
  INFERENCE_URL="https://us.inference.heroku.com" \
  INFERENCE_MODEL_ID="claude-4-5-sonnet" \
  INFERENCE_KEY="REDACTED" \
  -a $APP

# Optional: set APP_BASE_URL if you need stable OpenAPI server url
# heroku config:set APP_BASE_URL="$(heroku apps:info -a $APP | sed -n 's/^Web URL: //p' | tr -d '\r')" -a $APP

# Verify logs and health
heroku logs --tail -a $APP
```

Procfile (already present):
```
web: java $JAVA_OPTS -Dserver.port=$PORT -jar server/target/app.jar
```

Spring Boot applies `/server/src/main/resources/schema.sql` on startup:
- `transformed_code`, `execution_audit` (existing)
- `code_binding`, `job_queue`, `compiled_artifact` (new for runtime)

## Managed Inference (Streaming)
- Endpoint: `INFERENCE_URL=https://us.inference.heroku.com`
- Route: `/v1/chat/completions` with `stream=true`
- Auth: `Authorization: Bearer $INFERENCE_KEY`
- Model: `INFERENCE_MODEL_ID=claude-4-5-sonnet`
The app uses `WebClient` to stream tokens and aggregates the result. Retries once on transient errors.

## Runtime Automation (Approve → Publish)
1) Approve writes to `transformed_code`, upserts a default web binding `/exec/{name}`, and enqueues `compile` then `publish` jobs into `job_queue` (and emits a NOTIFY tick).
2) `QueueWorker` compiles (and optionally caches) and calls `PublishService`.
3) `OpenApiService` generates dynamic OpenAPI from `code_binding` (web) plus aliases and logs the ready‑to‑publish spec. You can wire this to AppLink publish if desired.

## Calling the Code
- Web: `POST /exec/{name}` body `{ "payload": {} }`
- Queue: `POST /runtime/job/enqueue { "name":"MyJob", "payload":{} }` (worker picks it up)
- Trigger (simulated): `NOTIFY mia_events, '{"job_type":"execute","target_name":"MyJob","payload":{}}'`

## Salesforce Integration
1) Named Credential → your Heroku app base URL
2) External Services → import from `/runtime/openapi.yaml`
3) Flow → add actions for `/exec/{name}`

## Troubleshooting
- 408 from Inference: streaming mitigates; ensure `INFERENCE_*` vars are set and model is valid.
- DB schema: check startup logs for schema.sql application.
- OpenAPI: hit `/runtime/openapi.yaml` to inspect generated spec.

## Paths
- UI: `server/src/main/resources/static/index.html`
- Inference client: `server/src/main/java/com/alansf/apexzombiekiller/service/InferenceClient.java`
- Approve/Execute: `server/src/main/java/com/alansf/apexzombiekiller/controller/*`
- Workers: `server/src/main/java/com/alansf/apexzombiekiller/worker/*`
- OpenAPI: `server/src/main/java/com/alansf/apexzombiekiller/service/OpenApiService.java`

## Apex Zombie Killer

Demo app to offload/transform Apex to Java/JS, run approved code on Heroku, and expose actions to Salesforce via AppLink in user-plus mode. Includes a simple UI, approval flow, dynamic OpenAPI, and LWC embedding.

App/Org
- Heroku app: `apex-zombie-killer`
- Base URL (live): `https://apex-zombie-killer-6f48e437a14e.herokuapp.com`
- Salesforce org alias: `purple-zombie` (`https://purple-zombie.my.salesforce.com`)

### 1) Build and deploy (from repo root)
```bash
cd /Users/alan.scott/Development/apex-zombie-killer
mvn -q -DskipTests -f pom.xml clean package
heroku buildpacks:clear -a apex-zombie-killer
heroku buildpacks:add heroku/java -a apex-zombie-killer
git fetch heroku main || true
git push heroku main || git push heroku main --force
```

### 2) Add-ons and config (demo-friendly)
```bash
# Postgres
heroku addons:create heroku-postgresql:standard-0 -a apex-zombie-killer

# AppLink add-on (SSO/trust path)
heroku addons:create heroku-applink:demo -a apex-zombie-killer

# Managed Inference (demo; use your real values)
heroku config:set INFERENCE_URL="https://us.inference.heroku.com" -a apex-zombie-killer
heroku config:set INFERENCE_MODEL_ID="claude-4-5-sonnet" -a apex-zombie-killer
heroku config:set INFERENCE_KEY="<your_inference_key>" -a apex-zombie-killer

# Optional AppLink API vars for programmatic orchestration (do not commit secrets)
heroku config:set HEROKU_APPLINK_API_URL="<applink_api_url>" -a apex-zombie-killer
heroku config:set HEROKU_APPLINK_TOKEN="<applink_token>" -a apex-zombie-killer
```

### 3) Salesforce deploy (Named Credential, CSP, Invocable, LWC)
```bash
sf org login web --instance-url https://purple-zombie.my.salesforce.com --alias purple-zombie --set-default
sf project deploy start --source-dir force-app --target-org purple-zombie
sf org assign permset --name ManageHerokuAppLink --target-org purple-zombie
```
- Named Credential endpoint: `https://apex-zombie-killer-6f48e437a14e.herokuapp.com`
- CSP Trusted Site (frame-src enabled): `https://apex-zombie-killer-6f48e437a14e.herokuapp.com`

### 4) Publish AppLink (user-plus)
```bash
cd /Users/alan.scott/Development/apex-zombie-killer
heroku salesforce:publish apispec.yaml \
  --client-name HerokuAPI \
  --connection-name purple-zombie \
  --authorization-connected-app-name "MyAppLinkApp" \
  --authorization-permission-set-name "ManageHerokuAppLink" \
  -a apex-zombie-killer
```

### 5) Link app & embed UI
- Setup → Heroku → Link App → `apex-zombie-killer`
- App Builder → add `herokuAppContainer` to an App Page (full‑width) or Record Page; set `appUrl` to the live Base URL.
- LWC styling updated for Heroku colors (purple primary, teal accent) and full‑bleed iframe.

### 6) Dynamic OpenAPI and External Services

**OpenAPI is dynamic-only** (no static file writes). The spec is generated from `code_binding` entries and served at `/openapi-generated.yaml`. After each Approve & Publish, the spec updates automatically.

**Import External Services:**
1) Approve code in the UI (e.g., name `ConvertedFromApex`) → wait for publish job to finish (check logs).
2) Setup → External Services → Add Service → Import from URL:
   ```
   https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi-generated.yaml
   ```
3) Named Credential: `HerokuJobs`
4) After import, you'll see actions like `exec_ConvertedFromApex` available in Flow Builder.

**Direct REST calls (no External Services needed):**
```bash
APP=https://apex-zombie-killer-6f48e437a14e.herokuapp.com

# Direct exec endpoint (internal)
curl -X POST "$APP/exec/ConvertedFromApex" \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"hello":"world"}}'

# External alias (matches OpenAPI operationId)
curl -X POST "$APP/ext/ConvertedFromApex/run" \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"hello":"world"}}'
```

### 7) Flow Templates and Deployment

**Pre-built Flow templates** are included in `force-app/main/default/flows/`:
- `ExecByName_Screen.flow-meta.xml` — Minimal Screen Flow template (variables + placeholder)
- `ExecByName_Auto.flow-meta.xml` — Minimal Autolaunched Flow template (variables + placeholder)

**Note:** These are minimal deployable flows without screen fields or External Service actions. They provide the basic structure and variables. You'll enhance them in Flow Builder after deployment.

**Deploy flows:**
```bash
cd /Users/alan.scott/Development/apex-zombie-killer

# Deploy minimal flow structure
sf project deploy start --source-dir force-app --target-org purple-zombie
```

**Complete flow setup (after External Service import):**
1) **Import External Service** (see step 6) — this creates actions like `exec_ConvertedFromApex`
2) **Open Flow Builder**: Setup → Flows → Edit `ExecByName_Screen` or `ExecByName_Auto`
3) **Enhance the flow**:
   - **For Screen Flow**: Add a Screen element with input field for `PayloadJSON`, then add External Service action
   - **For Auto Flow**: Replace the placeholder assignment with External Service action
   - **Add External Service action**: 
     - Action type: External Service
     - Select action: `exec_ConvertedFromApex` (or your approved code name)
     - Map input: `payload` → `PayloadJSON` variable (or `{}` for Auto)
     - Map outputs: `status` → `ResultStatus`, `error` → `ResultError` (if needed)
   - **For Screen Flow**: Add a result screen to display `ResultStatus`
4) **Save and Activate** the flow

**Run flows:**
- **Screen Flow**: Launch from App Launcher or add to a Record Page → user enters payload JSON → sees result.
- **Autolaunched Flow**: Invoke via Apex (`Flow.Interview.ExecByName_Auto.start(...)`) or as a subflow.

**End-to-end demo:**
1) Open Salesforce → "Heroku Transformer" page (embedded LWC).
2) Paste Apex → Transform → enter Name (e.g., `ConvertedFromApex`) → Approve & Publish.
3) Import External Service from `/openapi-generated.yaml` (see step 6).
4) Deploy flows: `sf project deploy start --source-dir force-app --target-org purple-zombie`
5) Edit flows in Flow Builder to add screens and External Service action `exec_ConvertedFromApex`.
6) Activate flows in Setup → Flows.
7) Run Screen Flow → enter payload → see execution result.

### 8) Schema (no psql required)
- The app creates tables on startup. If you prefer migrations:
  - Add Flyway and `db/migration/V1__init.sql` or use Spring `schema.sql` with `spring.sql.init.mode=always`

### 9) Paste‑ready Apex snippets

These avoid custom fields and use standard objects; ideal for quick transforms.

1) ProductPurchaseProcessBatchABM
```apex
public with sharing class ProductPurchaseProcessBatchABM implements Database.Batchable<SObject>{
  public Database.QueryLocator start(Database.BatchableContext bc){
    return Database.getQueryLocator('SELECT Id, Quantity, TotalPrice FROM OpportunityLineItem WHERE Opportunity.IsClosed = false');
  }
  public void execute(Database.BatchableContext bc, List<OpportunityLineItem> scope){
    Decimal totalQty = 0; Decimal totalRevenue = 0;
    for (OpportunityLineItem oli : scope){
      totalQty += (oli.Quantity == null ? 0 : oli.Quantity);
      totalRevenue += (oli.TotalPrice == null ? 0 : oli.TotalPrice);
    }
    System.debug('Processed OLI batch — qty=' + totalQty + ', revenue=' + totalRevenue);
  }
  public void finish(Database.BatchableContext bc){ System.debug('ProductPurchaseProcessBatchABM finished'); }
}
```

2) RevenueFileImportJob (mock file parse)
```apex
public with sharing class RevenueFileImportJob implements Queueable {
  public void execute(QueueableContext qc){
    List<String> lines = new List<String>{'Opp,Amount','A-001,1000','A-002,2500'};
    Decimal sum = 0;
    for (Integer i = 1; i < lines.size(); i++){
      List<String> cols = lines[i].split(',');
      sum += Decimal.valueOf(cols[1]);
    }
    System.debug('Imported revenue total = ' + sum);
  }
}
```

3) AccountPlanReportingDataBatch
```apex
public with sharing class AccountPlanReportingDataBatch implements Database.Batchable<SObject>{
  public Database.QueryLocator start(Database.BatchableContext bc){
    return Database.getQueryLocator('SELECT Id, Name, Type FROM Account WHERE IsDeleted = false');
  }
  public void execute(Database.BatchableContext bc, List<Account> scope){
    Integer cnt = 0; for (Account a : scope){ cnt++; }
    System.debug('Account plan rollup — batchCount=' + cnt);
  }
  public void finish(Database.BatchableContext bc){ System.debug('AccountPlanReportingDataBatch finished'); }
}
```

4) CPQ_QuoteOpprtunitySyncBatch
```apex
public with sharing class CPQ_QuoteOpprtunitySyncBatch implements Database.Batchable<SObject>{
  public Database.QueryLocator start(Database.BatchableContext bc){
    return Database.getQueryLocator('SELECT Id, OpportunityId, GrandTotal FROM Quote WHERE Status = \\'Approved\\'');
  }
  public void execute(Database.BatchableContext bc, List<Quote> scope){
    Decimal approvedTotal = 0;
    for (Quote q : scope){ approvedTotal += (q.GrandTotal == null ? 0 : q.GrandTotal); }
    System.debug('Approved quotes total = ' + approvedTotal);
  }
  public void finish(Database.BatchableContext bc){ System.debug('CPQ_QuoteOpprtunitySyncBatch finished'); }
}
```

5) OpportunityAndSplitBatch
```apex
public with sharing class OpportunityAndSplitBatch implements Database.Batchable<SObject>{
  public Database.QueryLocator start(Database.BatchableContext bc){
    return Database.getQueryLocator('SELECT Id, Amount, StageName FROM Opportunity WHERE IsClosed = false');
  }
  public void execute(Database.BatchableContext bc, List<Opportunity> scope){
    for (Opportunity o : scope){
      Decimal totalPct = 100; // placeholder
      System.debug('Opportunity ' + o.Id + ' split check %=' + totalPct);
    }
  }
  public void finish(Database.BatchableContext bc){ System.debug('OpportunityAndSplitBatch finished'); }
}
```

### 10) Notes
- For the embedded LWC demo we run without the AppLink Service Mesh so the iframe renders anonymously. To enforce SSO later, switch Procfile back to mesh and embed using an AppLink‑authenticated URL.
- **Dynamic OpenAPI**: Generated from `code_binding` entries; served at `/openapi-generated.yaml`. No static file writes. After Approve & Publish, the spec updates automatically (no app redeploy needed).
- **Exec endpoints**: `POST /exec/{name}` is the internal execution entrypoint; `/ext/{name}/run` is the External Services alias (both use the same `code_binding` mapping).
- **Flow templates**: Use placeholder `exec__PLACEHOLDER__` that gets patched to `exec_<name>` before deploy. This keeps flows reusable across different approved code names.




