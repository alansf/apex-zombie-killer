# Apex Zombie Killer – Single‑App Multi‑Modal Runtime (Postgres‑backed)

Demo‑ready Spring Boot app that:
- Transforms Apex → Java/JS using Heroku Managed Inference
- Approves code, binds it to runtime endpoints, and publishes a dynamic OpenAPI for Salesforce External Services
- Executes code via web, queue, or simulated Postgres trigger paths
- **Salesforce Flow Integration**: Pre-built Screen and Autolaunched Flow templates for executing approved code

## Key Capabilities

### Code Transformation & Execution
- ✅ AI-powered Apex → Java/JS transformation (Heroku Managed Inference)
- ✅ Code approval workflow with automatic OpenAPI generation
- ✅ Dynamic code execution (Java in-memory compilation, JS eval)
- ✅ Execution audit and logging

### Salesforce Integration
- ✅ **External Services**: Dynamic OpenAPI import for Flow Builder actions
- ✅ **Flow Templates**: Pre-built Screen and Autolaunched flows
- ✅ **LWC Embedding**: Full Heroku UI embedded in Lightning pages
- ✅ **Named Credentials**: Secure connection to Heroku endpoints

### Flow Capabilities
- ✅ **Screen Flow**: User-facing flows with input/output screens
- ✅ **Autolaunched Flow**: Background automation for programmatic execution
- ✅ **External Service Actions**: Call `exec_{name}` actions from approved code
- ✅ **Error Handling**: Built-in fault paths and error management
- ✅ **Reusable**: Flows can be called from App Launcher, Record Pages, Apex, or other Flows

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

### Components
1. **Named Credential** (`HerokuJobs`) → Heroku app base URL
2. **CSP Trusted Site** → Allows iframe embedding in LWC
3. **External Services** → Import from `/openapi-generated.yaml`
4. **Flow Templates** → Pre-built Screen and Autolaunched flows
5. **LWC Component** (`herokuAppContainer`) → Embeds Heroku UI in Lightning pages

### Integration Flow
```
Apex Code → Transform → Approve → External Service Import → Flow Enhancement → Execute
```

See sections 6-8 for detailed setup instructions.

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

### 7) Flow Templates and Capabilities

#### Flow Templates Overview

**Pre-built Flow templates** are included in `force-app/main/default/flows/`:
- **`ExecByName_Screen.flow-meta.xml`** — Screen Flow template
  - **Purpose**: Interactive user-facing flow for executing approved code
  - **Variables**: `PayloadJSON` (input), `ResultStatus`, `ResultError`
  - **Use Cases**: App Launcher, Record Pages, User-initiated actions
  - **Enhancement**: Add input screen, External Service action, result screen in Flow Builder

- **`ExecByName_Auto.flow-meta.xml`** — Autolaunched Flow template
  - **Purpose**: Background automation for programmatic execution
  - **Variables**: `ResultStatus`
  - **Use Cases**: Apex invocation, Subflows, Scheduled automation
  - **Enhancement**: Replace placeholder with External Service action in Flow Builder

**Note:** These are minimal deployable flows (variables + placeholder) to avoid metadata validation errors. They provide the foundation - enhance them visually in Flow Builder where field types are handled automatically.

#### Flow Capabilities

**Screen Flow Capabilities:**
- ✅ **User Input Collection**: Text Area field for JSON payload input
- ✅ **External Service Integration**: Call `exec_{name}` actions from approved code
- ✅ **Result Display**: Show execution status and errors to users
- ✅ **Error Handling**: Fault paths and error variable mapping
- ✅ **Navigation**: Back/Finish buttons for user flow control
- ✅ **Record Context**: Access to record variables when added to Record Pages

**Autolaunched Flow Capabilities:**
- ✅ **Programmatic Execution**: Invoke from Apex or other Flows
- ✅ **Background Processing**: No user interaction required
- ✅ **Subflow Support**: Reusable across multiple parent flows
- ✅ **Error Handling**: Fault paths for graceful error management
- ✅ **Variable Passing**: Accept inputs from calling context

#### Deploy Flows

```bash
cd /Users/alan.scott/Development/apex-zombie-killer

# Deploy minimal flow structure
sf project deploy start --source-dir force-app --target-org purple-zombie
```

#### Enhance Flows in Flow Builder

**After External Service Import:**

1. **Open Flow Builder**: Setup → Flows → Edit `ExecByName_Screen` or `ExecByName_Auto`

2. **For Screen Flow** (`ExecByName_Screen`):
   - **Add Input Screen**:
     - Click "+" → Select "Screen"
     - Add field → Variable → `PayloadJSON`
     - Field type: Text Area (for JSON input)
     - Label: "Payload (JSON)"
   - **Add External Service Action**:
     - Click "+" after input screen
     - Action → External Service → Select `exec_ConvertedFromApex`
     - Map `payload` input → `{!PayloadJSON}`
     - Map outputs: `status` → `{!ResultStatus}`, `error` → `{!ResultError}`
   - **Add Result Screen**:
     - Click "+" after External Service action
     - Add Display Text fields showing `{!ResultStatus}` and `{!ResultError}`
   - **Connect**: Start → Input Screen → External Service → Result Screen → End

3. **For Autolaunched Flow** (`ExecByName_Auto`):
   - **Replace Placeholder**:
     - Delete "Placeholder Assignment" element
     - Add External Service action
     - Select `exec_ConvertedFromApex`
     - Map `payload` → `{}` (empty object) or use input variable
     - Map outputs → `{!ResultStatus}`
   - **Add Error Handling** (optional):
     - Add fault path from External Service action
     - Set error variables or log errors

4. **Save and Activate**: Click Save → Activate

**See `FLOW_SETUP_GUIDE.md` for detailed step-by-step instructions with visual diagrams.**

#### Running Flows

**Screen Flow:**
```bash
# Launch from App Launcher
# Search for "Execute Code by Name (Screen)"
# Enter payload: {"test":"data"}
# Click Next → See execution result
```

**Autolaunched Flow:**
```apex
// Invoke from Apex
Map<String, Object> inputs = new Map<String, Object>();
Flow.Interview.ExecByName_Auto flow = new Flow.Interview.ExecByName_Auto(inputs);
flow.start();
```

**Or use as Subflow:**
- Add "Subflow" element in another Flow
- Select `ExecByName_Auto`
- Pass inputs if needed

### 8) Complete Demo Flow

#### Demo Scenario: Transform Apex → Execute via Flow

**Step 1: Transform & Approve Code**
1. Open Salesforce → Navigate to "Heroku Transformer" page (embedded LWC)
2. Paste Apex code (see examples in section 9)
3. Select target: **Java** or **JavaScript**
4. Click **"Transform"** → Review generated code in tabs (Java/JS/Test/Notes)
5. Enter name: `ConvertedFromApex` (or your preferred name)
6. Click **"Approve & Publish"**
   - ✅ Code saved to `transformed_code` table
   - ✅ Web binding created: `/exec/ConvertedFromApex`
   - ✅ Compile and publish jobs enqueued
   - ✅ OpenAPI spec updated automatically
   - ✅ Ready for External Service import

**Step 2: Import External Service**
1. Setup → External Services → **Add Service**
2. Import from URL:
   ```
   https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi-generated.yaml
   ```
3. Named Credential: `HerokuJobs`
4. After import, action `exec_ConvertedFromApex` appears in Flow Builder

**Step 3: Deploy & Enhance Flows**
1. Deploy flows:
   ```bash
   sf project deploy start --source-dir force-app --target-org purple-zombie
   ```
2. Open Flow Builder → Edit `ExecByName_Screen`
3. Enhance flow (see section 7 above)
4. Activate flow

**Step 4: Execute via Flow**
1. Launch Screen Flow from App Launcher
2. Enter payload JSON: `{"test":"data"}`
3. Click Next → Flow calls External Service action
4. Result screen displays execution status

**Step 5: Verify Execution**
- Check Heroku logs: `heroku logs --tail -a apex-zombie-killer`
- Check execution audit in database (if UI available)
- Verify result in Flow's result screen

#### Alternative: Direct REST Execution

If you prefer not to use Flows, you can call the endpoints directly:

```bash
APP=https://apex-zombie-killer-6f48e437a14e.herokuapp.com

# Direct exec endpoint
curl -X POST "$APP/exec/ConvertedFromApex" \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"hello":"world"}}'

# External alias (matches OpenAPI)
curl -X POST "$APP/ext/ConvertedFromApex/run" \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"hello":"world"}}'
```

#### Demo Notes

**Why Use Flows?**
- **No Code Required**: Visual Flow Builder interface
- **User-Friendly**: Screen Flows provide UI for non-technical users
- **Salesforce Native**: Integrated with Salesforce security and permissions
- **Reusable**: Flows can be called from multiple places
- **Error Handling**: Built-in fault paths and error management

**When to Use Each Flow Type:**
- **Screen Flow**: User-initiated actions, Record Page buttons, App Launcher
- **Autolaunched Flow**: Background automation, Scheduled jobs, Apex-triggered execution

**Best Practices:**
- Always test flows before activating
- Use meaningful variable names
- Add help text to screen fields
- Handle errors gracefully with fault paths
- Document your flows with descriptions
- Consider adding decision elements for conditional logic

### 9) Schema (no psql required)
- The app creates tables on startup. If you prefer migrations:
  - Add Flyway and `db/migration/V1__init.sql` or use Spring `schema.sql` with `spring.sql.init.mode=always`

### 10) Paste‑ready Apex snippets

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

### 11) Quick Reference: Demo Flow Checklist

**Complete End-to-End Demo:**

```bash
# 1. Transform & Approve (in Salesforce UI)
[ ] Open "Heroku Transformer" page
[ ] Paste Apex code → Transform → Approve & Publish

# 2. Import External Service (Salesforce Setup)
[ ] Setup → External Services → Add Service
[ ] Import from: https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi-generated.yaml
[ ] Named Credential: HerokuJobs

# 3. Deploy Flows (SFDX)
[ ] sf project deploy start --source-dir force-app --target-org purple-zombie

# 4. Enhance Flows (Flow Builder)
[ ] Setup → Flows → Edit ExecByName_Screen
[ ] Add input screen with PayloadJSON field
[ ] Add External Service action (exec_ConvertedFromApex)
[ ] Add result screen
[ ] Save and Activate

# 5. Execute Flow
[ ] Launch Screen Flow from App Launcher
[ ] Enter payload: {"test":"data"}
[ ] View execution result

# 6. Verify (Optional)
[ ] Check Heroku logs: heroku logs --tail -a apex-zombie-killer
[ ] Test direct REST: curl -X POST https://apex-zombie-killer-6f48e437a14e.herokuapp.com/exec/ConvertedFromApex -d '{"payload":{}}'
```

### 12) Notes
- For the embedded LWC demo we run without the AppLink Service Mesh so the iframe renders anonymously. To enforce SSO later, switch Procfile back to mesh and embed using an AppLink‑authenticated URL.
- **Dynamic OpenAPI**: Generated from `code_binding` entries; served at `/openapi-generated.yaml`. No static file writes. After Approve & Publish, the spec updates automatically (no app redeploy needed).
- **Exec endpoints**: `POST /exec/{name}` is the internal execution entrypoint; `/ext/{name}/run` is the External Services alias (both use the same `code_binding` mapping).
- **Flow templates**: Minimal deployable structures (variables + placeholder). Enhance in Flow Builder to avoid metadata enum validation issues. Flow Builder handles field types automatically.
- **Flow capabilities**: Screen Flows provide user-facing UI for interactive execution; Autolaunched Flows enable background automation and programmatic invocation.




