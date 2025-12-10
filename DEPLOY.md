# Deployment Commands

Quick reference for deploying the Apex Zombie Killer application.

## Quick Deploy (All)

```bash
cd /Users/alan.scott/Development/apex-zombie-killer
./scripts/deploy.sh all
```

## Heroku Deployment

### Full Heroku Deploy
```bash
cd /Users/alan.scott/Development/apex-zombie-killer

# Build
mvn -q -DskipTests -f pom.xml clean package

# Set buildpack (if needed)
heroku buildpacks:clear -a apex-zombie-killer
heroku buildpacks:add heroku/java -a apex-zombie-killer

# Deploy
git push heroku main

# View logs
heroku logs --tail -a apex-zombie-killer
```

### Heroku Only (using script)
```bash
./scripts/deploy.sh heroku
```

### Verify Heroku Deployment
```bash
APP=https://apex-zombie-killer-6f48e437a14e.herokuapp.com

# Health check
curl "$APP/actuator/health"

# OpenAPI spec
curl "$APP/openapi-generated.yaml"

# Test transform endpoint
curl -X POST "$APP/transform/apex-to-java" \
  -H 'Content-Type: application/json' \
  -d '{"apexCode":"public class Test { public void run() {} }"}'
```

## Salesforce Deployment

### Full Salesforce Deploy
```bash
cd /Users/alan.scott/Development/apex-zombie-killer

# Login (if needed)
sf org login web --instance-url https://purple-zombie.my.salesforce.com --alias purple-zombie --set-default

# Deploy metadata
sf project deploy start --source-dir force-app --target-org purple-zombie

# Assign permission set
sf org assign permset --name ManageHerokuAppLink --target-org purple-zombie
```

### Salesforce Only (using script)
```bash
./scripts/deploy.sh salesforce
```

### Deploy Flows (after patching)
```bash
# Patch flows with approved code name
./scripts/flow/patch-flow-op.sh ConvertedFromApex

# Deploy flows
sf project deploy start --source-dir force-app --target-org purple-zombie

# Activate flows in Setup → Flows
```

## Post-Deployment Steps

### 1. Import External Service
1. Setup → External Services → Add Service
2. Import from URL: `https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi-generated.yaml`
3. Named Credential: `HerokuJobs`

### 2. Patch and Deploy Flows
```bash
# Replace placeholder with your approved code name
./scripts/flow/patch-flow-op.sh ConvertedFromApex

# Deploy
sf project deploy start --source-dir force-app --target-org purple-zombie
```

### 3. Activate Flows
- Setup → Flows → Activate `ExecByName_Screen` and/or `ExecByName_Auto`

### 4. Test End-to-End
```bash
# Approve code in UI, then test via REST
APP=https://apex-zombie-killer-6f48e437a14e.herokuapp.com

curl -X POST "$APP/exec/ConvertedFromApex" \
  -H 'Content-Type: application/json' \
  -d '{"payload":{"test":"data"}}'
```

## Environment Variables (Heroku)

```bash
# Required
heroku config:set INFERENCE_URL="https://us.inference.heroku.com" -a apex-zombie-killer
heroku config:set INFERENCE_MODEL_ID="claude-4-5-sonnet" -a apex-zombie-killer
heroku config:set INFERENCE_KEY="<your_key>" -a apex-zombie-killer

# Optional
heroku config:set APP_BASE_URL="https://apex-zombie-killer-6f48e437a14e.herokuapp.com" -a apex-zombie-killer
heroku config:set HEROKU_APPLINK_API_URL="<applink_url>" -a apex-zombie-killer
heroku config:set HEROKU_APPLINK_TOKEN="<applink_token>" -a apex-zombie-killer
```

## Troubleshooting

### Heroku Build Fails
```bash
# Check build logs
heroku logs --tail -a apex-zombie-killer

# Verify buildpack
heroku buildpacks -a apex-zombie-killer

# Clear build cache and rebuild
heroku builds:cache:purge -a apex-zombie-killer
```

### Salesforce Deploy Fails
```bash
# Check deploy status
sf project deploy report --target-org purple-zombie

# Validate metadata
sf project deploy validate --source-dir force-app --target-org purple-zombie

# Check org connection
sf org display --target-org purple-zombie
```

### Database Schema Issues
```bash
# Check Postgres logs
heroku logs --tail -a apex-zombie-killer | grep -i postgres

# Verify schema initialization
heroku pg:psql -a apex-zombie-killer -c "\dt"
```

## Rollback

### Heroku Rollback
```bash
# List releases
heroku releases -a apex-zombie-killer

# Rollback to previous release
heroku rollback -a apex-zombie-killer
```

### Salesforce Rollback
```bash
# Retrieve previous version from org
sf project retrieve start --target-org purple-zombie

# Or redeploy specific components
sf project deploy start --source-dir force-app --target-org purple-zombie --ignore-warnings
```

