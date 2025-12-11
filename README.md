# Apex Zombie Killer

**Transform Salesforce Apex code to Java/JavaScript and execute it on Heroku with seamless Salesforce integration.**

A demo-ready Spring Boot application that demonstrates how to:
- Transform Apex → Java/JS using Heroku Managed Inference
- Approve and execute transformed code dynamically
- Expose execution endpoints via Salesforce External Services and Flows
- Embed the transformation UI directly in Salesforce Lightning pages

---

## Table of Contents

- [Overview](#overview)
- [Key Capabilities](#key-capabilities)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Deployment](#deployment)
- [Salesforce Integration](#salesforce-integration)
- [Flow Templates](#flow-templates)
- [Demo Flow](#demo-flow)
- [Apex Code Examples](#apex-code-examples)
- [Troubleshooting](#troubleshooting)
- [License & Disclaimer](#license--disclaimer)

---

## Overview

**Apex Zombie Killer** is a demonstration application that showcases how to:

1. **Transform Apex Code**: Use AI-powered transformation (Heroku Managed Inference) to convert Salesforce Apex batch classes and queueable jobs into standalone Java or JavaScript code
2. **Approve & Publish**: Review transformed code, approve it, and automatically generate dynamic OpenAPI specifications
3. **Execute Dynamically**: Run approved code via web endpoints, job queues, or Salesforce Flows
4. **Integrate with Salesforce**: Seamlessly connect to Salesforce via External Services, Flows, and Lightning Web Components

### What Problem Does This Solve?

- **Offload Heavy Processing**: Move compute-intensive Apex operations to Heroku
- **Leverage AI Transformation**: Automatically convert Apex patterns to optimized Java/JS
- **Dynamic Code Execution**: Execute transformed code without redeploying the application
- **Salesforce Integration**: Use familiar Salesforce tools (Flows, External Services) to trigger execution

---

## Key Capabilities

### Code Transformation & Execution
- ✅ **AI-Powered Transformation**: Apex → Java/JS using Heroku Managed Inference (Claude)
- ✅ **Code Approval Workflow**: Review, approve, and bind code to runtime endpoints
- ✅ **Dynamic OpenAPI Generation**: Automatically generate OpenAPI specs from approved code
- ✅ **In-Memory Execution**: Java dynamic compilation and JavaScript evaluation
- ✅ **Execution Auditing**: Track all code executions with audit logs

### Salesforce Integration
- ✅ **External Services**: Dynamic OpenAPI import for Flow Builder actions
- ✅ **Flow Templates**: Pre-built Screen and Autolaunched flows
- ✅ **LWC Embedding**: Full Heroku UI embedded in Lightning pages
- ✅ **Named Credentials**: Secure connection to Heroku endpoints
- ✅ **CSP Trusted Sites**: Configured for iframe embedding

### Flow Capabilities
- ✅ **Screen Flow**: User-facing flows with input/output screens
- ✅ **Autolaunched Flow**: Background automation for programmatic execution
- ✅ **External Service Actions**: Call `exec_{name}` actions from approved code
- ✅ **Error Handling**: Built-in fault paths and error management
- ✅ **Reusable**: Flows can be called from App Launcher, Record Pages, Apex, or other Flows

---

## Architecture

```
┌─────────────────┐
│   Salesforce    │
│                 │
│  ┌───────────┐  │
│  │   Flow    │──┼──► External Service ──┐
│  └───────────┘  │                       │
│                 │                       │
│  ┌───────────┐  │                       │
│  │    LWC    │──┼──► iframe ────────────┼──┐
│  └───────────┘  │                       │  │
└─────────────────┘                       │  │
                                            │  │
┌───────────────────────────────────────────┼──┼──┐
│           Heroku Application             │  │  │
│                                           │  │  │
│  ┌────────────────────────────────────┐   │  │  │
│  │  Transform UI (index.html)        │◄──┘  │  │
│  └────────────────────────────────────┘      │  │
│              │                                │  │
│              ▼                                │  │
│  ┌────────────────────────────────────┐       │  │
│  │  InferenceClient                  │       │  │
│  │  (Managed Inference API)           │       │  │
│  └────────────────────────────────────┘       │  │
│              │                                │  │
│              ▼                                │  │
│  ┌────────────────────────────────────┐       │  │
│  │  Code Approval & Binding           │       │  │
│  │  - transformed_code table           │       │  │
│  │  - code_binding table               │       │  │
│  └────────────────────────────────────┘       │  │
│              │                                │  │
│              ▼                                │  │
│  ┌────────────────────────────────────┐       │  │
│  │  OpenAPI Generation                │       │  │
│  │  - Dynamic spec generation          │       │  │
│  │  - /openapi-generated.yaml          │       │  │
│  └────────────────────────────────────┘       │  │
│              │                                │  │
│              ▼                                │  │
│  ┌────────────────────────────────────┐       │  │
│  │  Code Execution                     │◄──────┘  │
│  │  - POST /exec/{name}                │          │
│  │  - POST /ext/{name}/run             │          │
│  │  - JavaExecutionAdapter            │          │
│  └────────────────────────────────────┘          │
│                                                   │
│  ┌────────────────────────────────────┐          │
│  │  PostgreSQL                         │          │
│  │  - transformed_code                 │          │
│  │  - code_binding                    │          │
│  │  - execution_audit                  │          │
│  │  - job_queue                        │          │
│  └────────────────────────────────────┘          │
└───────────────────────────────────────────────────┘
```

---

## Quick Start

### Local Development

**Prerequisites:**
- Java 21+ and Maven
- PostgreSQL (or use Heroku Postgres locally)

```bash
# Clone and build
cd /Users/alan.scott/Development/apex-zombie-killer
mvn -f server/pom.xml -DskipTests package
java -jar server/target/app.jar
```

Open `http://localhost:8080/` and try a transform + Approve.

### Heroku Deployment

**Prerequisites:**
- Heroku account
- Heroku CLI installed
- Git repository initialized

```bash
APP=apex-zombie-killer

# Build and deploy
git add .
git commit -m "Deploy: Apex Zombie Killer"
git push heroku main

# Required config vars
heroku config:set \
  INFERENCE_URL="https://us.inference.heroku.com" \
  INFERENCE_MODEL_ID="claude-4-5-sonnet" \
  INFERENCE_KEY="<your_inference_key>" \
  -a $APP

# Verify deployment
heroku logs --tail -a $APP
heroku ps -a $APP
```

**Add-ons:**
```bash
# Postgres (required)
heroku addons:create heroku-postgresql:standard-0 -a $APP

# AppLink (optional, for SSO)
heroku addons:create heroku-applink:demo -a $APP
```

---

## Deployment

### Heroku Setup

**1. Build and Deploy**
```bash
cd /Users/alan.scott/Development/apex-zombie-killer
mvn -q -DskipTests -f pom.xml clean package
heroku buildpacks:clear -a apex-zombie-killer
heroku buildpacks:add heroku/java -a apex-zombie-killer
git push heroku main
```

**2. Configure Environment Variables**
```bash
# Managed Inference (required)
heroku config:set INFERENCE_URL="https://us.inference.heroku.com" -a apex-zombie-killer
heroku config:set INFERENCE_MODEL_ID="claude-4-5-sonnet" -a apex-zombie-killer
heroku config:set INFERENCE_KEY="<your_inference_key>" -a apex-zombie-killer

# Optional: AppLink API vars (for programmatic orchestration)
heroku config:set HEROKU_APPLINK_API_URL="<applink_api_url>" -a apex-zombie-killer
heroku config:set HEROKU_APPLINK_TOKEN="<applink_token>" -a apex-zombie-killer
```

**3. Database Schema**
The app automatically creates tables on startup via `schema.sql`:
- `transformed_code` - Approved code storage
- `code_binding` - Runtime endpoint mappings
- `execution_audit` - Execution history
- `job_queue` - Async job processing
- `compiled_artifact` - Compiled Java bytecode cache

### Salesforce Deployment

**1. Authenticate**
```bash
sf org login web --instance-url https://purple-zombie.my.salesforce.com --alias purple-zombie --set-default
```

**2. Deploy Metadata**
```bash
sf project deploy start --source-dir force-app --target-org purple-zombie
sf org assign permset --name ManageHerokuAppLink --target-org purple-zombie
```

**3. Publish AppLink (Optional)**
```bash
heroku salesforce:publish apispec.yaml \
  --client-name HerokuAPI \
  --connection-name purple-zombie \
  --authorization-connected-app-name "MyAppLinkApp" \
  --authorization-permission-set-name "ManageHerokuAppLink" \
  -a apex-zombie-killer
```

**4. Link App & Embed UI**
- Setup → Heroku → Link App → `apex-zombie-killer`
- App Builder → Add `herokuAppContainer` to an App Page or Record Page
- Set `appUrl` to: `https://apex-zombie-killer-6f48e437a14e.herokuapp.com/`

---

## Salesforce Integration

### Components

| Component | Purpose | Location |
|-----------|---------|----------|
| **Named Credential** | Secure connection to Heroku | `force-app/main/default/namedCredentials/` |
| **CSP Trusted Site** | Allow iframe embedding | `force-app/main/default/cspTrustedSites/` |
| **External Services** | Import OpenAPI for Flows | Setup → External Services |
| **Flow Templates** | Pre-built execution flows | `force-app/main/default/flows/` |
| **LWC Component** | Embed Heroku UI | `force-app/main/default/lwc/herokuAppContainer/` |

### Integration Flow

```
Apex Code → Transform → Approve → External Service Import → Flow Enhancement → Execute
```

### Dynamic OpenAPI

**OpenAPI is dynamic-only** (no static file writes). The spec is generated from `code_binding` entries and served at `/openapi-generated.yaml`. After each Approve & Publish, the spec updates automatically.

**Import External Services:**
1. Approve code in the UI (e.g., name `ConvertedFromApex`) → wait for publish job to finish
2. Setup → External Services → Add Service → Import from URL:
   ```
   https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi-generated.yaml
   ```
3. Named Credential: `HerokuJobs`
4. After import, actions like `exec_ConvertedFromApex` appear in Flow Builder

**Direct REST Calls:**
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

---

## Flow Templates

### Overview

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

### Flow Capabilities

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

### Deploy & Enhance Flows

**1. Deploy Flows**
```bash
cd /Users/alan.scott/Development/apex-zombie-killer
sf project deploy start --source-dir force-app --target-org purple-zombie
```

**2. Enhance in Flow Builder**

**For Screen Flow** (`ExecByName_Screen`):
1. Open Flow Builder: Setup → Flows → Edit `ExecByName_Screen`
2. **Add Input Screen**:
   - Click "+" → Select "Screen"
   - Add field → Variable → `PayloadJSON`
   - Field type: Text Area (for JSON input)
   - Label: "Payload (JSON)"
3. **Add External Service Action**:
   - Click "+" after input screen
   - Action → External Service → Select `exec_ConvertedFromApex`
   - Map `payload` input → `{!PayloadJSON}`
   - Map outputs: `status` → `{!ResultStatus}`, `error` → `{!ResultError}`
4. **Add Result Screen**:
   - Click "+" after External Service action
   - Add Display Text fields showing `{!ResultStatus}` and `{!ResultError}`
5. **Connect**: Start → Input Screen → External Service → Result Screen → End
6. **Save and Activate**

**For Autolaunched Flow** (`ExecByName_Auto`):
1. Open Flow Builder: Setup → Flows → Edit `ExecByName_Auto`
2. **Replace Placeholder**:
   - Delete "Placeholder Assignment" element
   - Add External Service action
   - Select `exec_ConvertedFromApex`
   - Map `payload` → `{}` (empty object) or use input variable
   - Map outputs → `{!ResultStatus}`
3. **Add Error Handling** (optional):
   - Add fault path from External Service action
   - Set error variables or log errors
4. **Save and Activate**

**See `FLOW_SETUP_GUIDE.md` for detailed step-by-step instructions with visual diagrams.**

### Running Flows

**Screen Flow:**
- Launch from App Launcher
- Search for "Execute Code by Name (Screen)"
- Enter payload: `{"test":"data"}`
- Click Next → See execution result

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

---

## Demo Flow

### Complete End-to-End Demo

**Step 1: Transform & Approve Code**
1. Open Salesforce → Navigate to "Heroku Transformer" page (embedded LWC)
2. Paste Apex code (see examples below)
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
3. Enhance flow (see Flow Templates section above)
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

### Demo Checklist

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

### Demo Notes

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

---

## Apex Code Examples

These snippets avoid custom fields and use standard objects; ideal for quick transforms.

### 1. ProductPurchaseProcessBatchABM
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

### 2. RevenueFileImportJob
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

### 3. AccountPlanReportingDataBatch
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

### 4. CPQ_QuoteOpprtunitySyncBatch
```apex
public with sharing class CPQ_QuoteOpprtunitySyncBatch implements Database.Batchable<SObject>{
  public Database.QueryLocator start(Database.BatchableContext bc){
    return Database.getQueryLocator('SELECT Id, OpportunityId, GrandTotal FROM Quote WHERE Status = \'Approved\'');
  }
  public void execute(Database.BatchableContext bc, List<Quote> scope){
    Decimal approvedTotal = 0;
    for (Quote q : scope){ approvedTotal += (q.GrandTotal == null ? 0 : q.GrandTotal); }
    System.debug('Approved quotes total = ' + approvedTotal);
  }
  public void finish(Database.BatchableContext bc){ System.debug('CPQ_QuoteOpprtunitySyncBatch finished'); }
}
```

### 5. OpportunityAndSplitBatch
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

---

## Troubleshooting

### Common Issues

**408 Timeout from Inference:**
- Streaming mitigates timeout issues
- Ensure `INFERENCE_URL`, `INFERENCE_MODEL_ID`, and `INFERENCE_KEY` are set correctly
- Verify model name is valid (e.g., `claude-4-5-sonnet`)

**Database Schema Issues:**
- Check startup logs for `schema.sql` application
- Verify Postgres addon is attached: `heroku addons -a apex-zombie-killer`
- Review connection string: `heroku config:get DATABASE_URL -a apex-zombie-killer`

**OpenAPI Generation:**
- Inspect generated spec: `curl https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi-generated.yaml`
- Verify code bindings exist: Check `code_binding` table after approval
- Review logs: `heroku logs --tail -a apex-zombie-killer | grep -i openapi`

**Flow Execution Errors:**
- Verify External Service is imported correctly
- Check Named Credential configuration
- Review Flow Builder error messages
- Test direct REST endpoint first: `curl -X POST https://apex-zombie-killer-6f48e437a14e.herokuapp.com/exec/{name}`

**LWC Embedding Issues:**
- Verify CSP Trusted Site is configured: Setup → CSP Trusted Sites
- Check iframe sandbox attributes in LWC component
- Review browser console for CSP errors
- Ensure `X-Frame-Options` headers allow Salesforce domains

### Debugging

**View Application Logs:**
```bash
heroku logs --tail -a apex-zombie-killer
```

**Check Database:**
```bash
heroku pg:psql -a apex-zombie-killer
# Then run: SELECT * FROM transformed_code; SELECT * FROM code_binding;
```

**Test Endpoints:**
```bash
APP=https://apex-zombie-killer-6f48e437a14e.herokuapp.com

# Health check
curl "$APP/actuator/health"

# OpenAPI spec
curl "$APP/openapi-generated.yaml"

# Transform test
curl -X POST "$APP/transform/apex-to-java" \
  -H 'Content-Type: application/json' \
  -d '{"apexCode":"public class Test { public void run() {} }"}'
```

---

## Technical Details

### Managed Inference (Streaming)
- **Endpoint**: `INFERENCE_URL=https://us.inference.heroku.com`
- **Route**: `/v1/chat/completions` with `stream=true`
- **Auth**: `Authorization: Bearer $INFERENCE_KEY`
- **Model**: `INFERENCE_MODEL_ID=claude-4-5-sonnet`
- The app uses `WebClient` to stream tokens and aggregates the result. Retries once on transient errors.

### Runtime Automation (Approve → Publish)
1. **Approve** writes to `transformed_code`, upserts a default web binding `/exec/{name}`, and enqueues `compile` then `publish` jobs into `job_queue` (and emits a NOTIFY tick).
2. **QueueWorker** compiles (and optionally caches) and calls `PublishService`.
3. **OpenApiService** generates dynamic OpenAPI from `code_binding` (web) plus aliases and logs the ready-to-publish spec.

### Calling the Code
- **Web**: `POST /exec/{name}` body `{ "payload": {} }`
- **Queue**: `POST /runtime/job/enqueue { "name":"MyJob", "payload":{} }` (worker picks it up)
- **Trigger (simulated)**: `NOTIFY mia_events, '{"job_type":"execute","target_name":"MyJob","payload":{}}'`

### Key Paths
- **UI**: `server/src/main/resources/static/index.html`
- **Inference client**: `server/src/main/java/com/alansf/apexzombiekiller/service/InferenceClient.java`
- **Approve/Execute**: `server/src/main/java/com/alansf/apexzombiekiller/controller/*`
- **Workers**: `server/src/main/java/com/alansf/apexzombiekiller/worker/*`
- **OpenAPI**: `server/src/main/java/com/alansf/apexzombiekiller/service/OpenApiService.java`

### Important Notes
- **Dynamic OpenAPI**: Generated from `code_binding` entries; served at `/openapi-generated.yaml`. No static file writes. After Approve & Publish, the spec updates automatically (no app redeploy needed).
- **Exec endpoints**: `POST /exec/{name}` is the internal execution entrypoint; `/ext/{name}/run` is the External Services alias (both use the same `code_binding` mapping).
- **Flow templates**: Minimal deployable structures (variables + placeholder). Enhance in Flow Builder to avoid metadata enum validation issues. Flow Builder handles field types automatically.
- **LWC Embedding**: For the embedded LWC demo we run without the AppLink Service Mesh so the iframe renders anonymously. To enforce SSO later, switch Procfile back to mesh and embed using an AppLink-authenticated URL.

---

## License & Disclaimer

### License

MIT License

### Disclaimer

This software is to be considered **"sample code"**, a Type B Deliverable, and is delivered **"as-is"** to the user. Salesforce bears no responsibility to support the use or implementation of this software.

**Important Notes:**
- This is a **demonstration application** intended for learning and evaluation purposes
- Not intended for production use without thorough security review and testing
- No warranty or support is provided
- Use at your own risk

---

## Resources

- [Heroku Managed Inference Documentation](https://devcenter.heroku.com/articles/managed-inference)
- [Salesforce External Services](https://help.salesforce.com/s/articleView?id=sf.flow_external_services.htm)
- [Salesforce Flow Builder](https://help.salesforce.com/s/articleView?id=sf.flow.htm)
- [Heroku AppLink](https://devcenter.heroku.com/articles/applink)
- [Model Context Protocol (MCP)](https://modelcontextprotocol.io/)

---

## Contributing

This is a demonstration project. Contributions, suggestions, and feedback are welcome!

---

**Built with ❤️ using Spring Boot, Heroku, and Salesforce**
