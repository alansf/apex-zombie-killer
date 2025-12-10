# Apex Zombie Killer - Application Plan

## Overview

Single-app multi-modal runtime on Heroku that transforms Apex code to Java/JS, executes approved code, and exposes actions to Salesforce via External Services and Flows.

## Architecture

### Core Components
- **Spring Boot Application**: Runs on Heroku, handles transformation, execution, and API endpoints
- **Postgres Database**: Stores transformed code, bindings, job queue, and execution audit
- **Dynamic OpenAPI**: Generated from `code_binding` entries, served at `/openapi-generated.yaml`
- **Salesforce Integration**: External Services, Flows, Named Credentials, LWC embedding

### Runtime Modes
1. **Web Execution**: `POST /exec/{name}` - Direct REST invocation
2. **Queue Execution**: Jobs enqueued to `job_queue`, processed by `QueueWorker`
3. **Trigger Simulation**: Postgres `LISTEN/NOTIFY` for event-driven execution

## Key Capabilities

### 1. Code Transformation
- **Apex → Java**: Transforms to Spring Boot-compatible Java with Heroku Postgres
- **Apex → JavaScript**: Transforms to Node.js-compatible JavaScript
- **AI-Powered**: Uses Heroku Managed Inference (Claude 4.5 Sonnet) for transformation
- **Streaming**: WebClient SSE support for long-running transformations
- **Fallback**: Stub generation when inference is unavailable

### 2. Code Approval & Publishing
- **Approval Flow**: UI-based approval with name assignment
- **Automatic Binding**: Creates web binding (`/exec/{name}`) on approval
- **Job Queue**: Enqueues `compile` and `publish` jobs automatically
- **Dynamic OpenAPI**: Updates spec automatically after each approval
- **AppLink Publishing**: Ready for Heroku AppLink integration

### 3. Dynamic Code Execution
- **Java Execution**: In-memory compilation using Janino/JSR-199
  - Auto-detects class names
  - Strips package declarations
  - Supports `run()`, `execute()`, or `main(String[])` entrypoints
- **JavaScript Execution**: Eval-on-demand (GraalJS not available in Heroku dyno)
- **Execution Audit**: Logs all executions with status, errors, and input/output

### 4. Salesforce Integration

#### External Services
- **Dynamic OpenAPI**: Generated from approved code bindings
- **Import Process**: 
  1. Approve code in UI
  2. Import External Service from `/openapi-generated.yaml`
  3. Actions available as `exec_{name}` in Flow Builder
- **Named Credential**: `HerokuJobs` configured for anonymous access

#### Flow Templates
- **ExecByName_Screen**: Minimal Screen Flow template
  - Variables: `PayloadJSON` (input), `ResultStatus`, `ResultError`
  - Placeholder assignment for External Service action
  - Deployable structure, enhance in Flow Builder
- **ExecByName_Auto**: Minimal Autolaunched Flow template
  - Variables: `ResultStatus`
  - Placeholder assignment for External Service action
  - For programmatic invocation

#### Flow Enhancement (Post-Deployment)
1. **Screen Flow Setup**:
   - Add input screen with `PayloadJSON` field (Text Area)
   - Add External Service action (`exec_{name}`)
   - Map `payload` input → `PayloadJSON` variable
   - Map outputs → `ResultStatus`, `ResultError`
   - Add result screen to display status
2. **Autolaunched Flow Setup**:
   - Replace placeholder with External Service action
   - Map inputs/outputs directly
   - Handle errors gracefully

#### LWC Embedding
- **herokuAppContainer**: Lightning Web Component for embedding Heroku UI
- **Full-bleed iframe**: Responsive design with Heroku branding
- **CSP Trusted Site**: Configured for iframe embedding
- **Open in New Tab**: Button for full-screen experience

## Demo Flow

### End-to-End Demo Steps

#### 1. Transform & Approve Code
```
1. Open Salesforce → "Heroku Transformer" page (embedded LWC)
2. Paste Apex code snippet
3. Select target: Java or JavaScript
4. Click "Transform" → Review generated code
5. Enter name (e.g., "ConvertedFromApex")
6. Click "Approve & Publish"
   - Code saved to `transformed_code` table
   - Web binding created: `/exec/ConvertedFromApex`
   - Compile and publish jobs enqueued
   - OpenAPI spec updated automatically
```

#### 2. Import External Service
```
1. Setup → External Services → Add Service
2. Import from URL:
   https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi-generated.yaml
3. Named Credential: HerokuJobs
4. After import, action `exec_ConvertedFromApex` available in Flow Builder
```

#### 3. Deploy & Enhance Flows
```
1. Deploy flows:
   sf project deploy start --source-dir force-app --target-org purple-zombie

2. Open Flow Builder → Edit ExecByName_Screen:
   - Add input screen with PayloadJSON field
   - Add External Service action (exec_ConvertedFromApex)
   - Map payload input → PayloadJSON variable
   - Map outputs → ResultStatus, ResultError
   - Add result screen to display status
   - Save and Activate

3. For ExecByName_Auto:
   - Replace placeholder with External Service action
   - Map inputs/outputs
   - Save and Activate
```

#### 4. Execute via Flow
```
Screen Flow:
- Launch from App Launcher or Record Page
- User enters payload JSON: {"test":"data"}
- Flow calls External Service action
- Result screen displays execution status

Autolaunched Flow:
- Invoke from Apex:
  Flow.Interview.ExecByName_Auto.start(new Map<String, Object>());
- Or use as subflow in other Flows
```

#### 5. Direct REST Execution (Alternative)
```bash
# Direct exec endpoint
curl -X POST https://apex-zombie-killer-6f48e437a14e.herokuapp.com/exec/ConvertedFromApex \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"hello":"world"}}'

# External alias (matches OpenAPI)
curl -X POST https://apex-zombie-killer-6f48e437a14e.herokuapp.com/ext/ConvertedFromApex/run \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"hello":"world"}}'
```

## Database Schema

### Tables
- **transformed_code**: Approved code snippets with metadata
- **execution_audit**: Execution history and results
- **code_binding**: Runtime bindings (web, trigger, queue)
- **job_queue**: Background job processing
- **compiled_artifact**: Cached compiled code (optional)

### Auto-Initialization
- Schema created automatically on startup via `schema.sql`
- Spring Boot `spring.sql.init.mode=always` enabled
- No manual migrations required

## Deployment

### Heroku
```bash
# Build and deploy
mvn -q -DskipTests -f pom.xml clean package
git push heroku main

# Or use script
./scripts/deploy.sh heroku
```

### Salesforce
```bash
# Deploy metadata
sf project deploy start --source-dir force-app --target-org purple-zombie

# Or use script
./scripts/deploy.sh salesforce
```

### Push to Both
```bash
# Push to origin and Heroku
./scripts/push-all.sh "Commit message"
```

## Configuration

### Required Environment Variables
- `INFERENCE_URL`: Heroku Managed Inference endpoint
- `INFERENCE_MODEL_ID`: Model identifier (e.g., claude-4-5-sonnet)
- `INFERENCE_KEY`: API key for inference service
- `DATABASE_URL`: Heroku Postgres connection string (auto-set)

### Optional Environment Variables
- `APP_BASE_URL`: Base URL for OpenAPI server (defaults to Heroku app URL)
- `HEROKU_APPLINK_API_URL`: AppLink API endpoint (for programmatic publishing)
- `HEROKU_APPLINK_TOKEN`: AppLink authentication token

## API Endpoints

### Transformation
- `POST /transform/apex-to-java` - Transform Apex to Java
- `POST /transform/apex-to-js` - Transform Apex to JavaScript

### Code Management
- `POST /code/approve` - Approve and publish code
- `GET /code/list` - List all approved code
- `GET /code/{name}` - Get code by name

### Execution
- `POST /exec/{name}` - Execute code by name (internal)
- `POST /ext/{name}/run` - Execute code (External Services alias)

### OpenAPI
- `GET /openapi-generated.yaml` - Dynamic OpenAPI specification

### Health
- `GET /actuator/health` - Application health check

## Flow Demo Capabilities

### Screen Flow Demo
- **User Input**: Collect payload JSON via screen
- **External Service Call**: Invoke `exec_{name}` action
- **Result Display**: Show execution status and errors
- **Navigation**: Back/Finish buttons for user flow
- **Use Cases**: App Launcher, Record Pages, User-initiated actions

### Autolaunched Flow Demo
- **Programmatic Execution**: Called from Apex or other Flows
- **No User Interaction**: Runs in background
- **Error Handling**: Fault paths and error variables
- **Use Cases**: Scheduled automation, Subflows, Triggered flows

### Flow Enhancement Process
1. **Deploy Minimal Structure**: Basic variables and placeholder
2. **Import External Service**: Makes actions available
3. **Enhance in Flow Builder**: Add screens, actions, logic visually
4. **Test & Activate**: Validate flow, then activate for use

## Troubleshooting

### Flow Deployment Issues
- **Enum Errors**: Use minimal flows, enhance in Flow Builder
- **Action Not Found**: Ensure External Service is imported first
- **Variable Errors**: Check variable names and scope

### Heroku Deployment Issues
- **Build Failures**: Check Maven dependencies and Java version
- **Database Errors**: Verify Postgres addon is attached
- **Inference Errors**: Check environment variables and model availability

### Salesforce Integration Issues
- **External Service Import**: Verify Named Credential is configured
- **Flow Activation**: Check all required fields are mapped
- **LWC Embedding**: Verify CSP Trusted Site is active

## Future Enhancements

### Planned Features
- **Flow Builder Integration**: Pre-built flows with screens (after enum issues resolved)
- **AppLink Publishing**: Automated OpenAPI publishing via AppLink API
- **Queue Management UI**: Visual interface for job queue monitoring
- **Execution Analytics**: Dashboard for execution metrics and performance
- **Multi-tenant Support**: Organization-specific code isolation

### Known Limitations
- **Flow Metadata**: Screen field types require Flow Builder enhancement
- **JavaScript Execution**: GraalJS not available in Heroku dyno environment
- **AppLink Mesh**: Currently disabled for anonymous iframe access

## Documentation

- **README.md**: Main project documentation
- **DEPLOY.md**: Deployment commands and procedures
- **FLOW_SETUP_GUIDE.md**: Detailed Flow Builder setup instructions
- **DEMO-RUNBOOK.md**: End-to-end demo runbook

## Status

✅ **Completed**:
- Dynamic OpenAPI generation
- Code transformation (Java/JS)
- Approval and publishing workflow
- Flow templates (minimal, deployable)
- External Services integration
- LWC embedding
- Execution endpoints
- Job queue system

🔄 **In Progress**:
- Flow enhancement documentation
- AppLink publishing automation

📋 **Planned**:
- Enhanced Flow templates with screens
- Queue management UI
- Execution analytics dashboard

