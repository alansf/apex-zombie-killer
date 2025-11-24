## Apex Zombie Killer

Demo app to offload/transform Apex to Java/JS, run approved code on Heroku, and expose actions to Salesforce via AppLink in user-plus mode. Includes a simple UI, approval flow, dynamic OpenAPI, and LWC embedding.

App/Org
- Heroku app: `apex-zombie-killer`
- Base URL: `https://apex-zombie-killer.herokuapp.com`
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
- Named Credential endpoint: `https://apex-zombie-killer.herokuapp.com`
- CSP Trusted Site: `https://apex-zombie-killer.herokuapp.com`

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
- App Builder → add `herokuAppContainer` to a page; optionally set `appUrl` to the base URL

### 6) External Services (optional)
- Setup → External Services → Add Service → `https://apex-zombie-killer.herokuapp.com/openapi.yaml`
- Named Credential: `HerokuJobs`

### 7) Demo flow
1) In the Heroku UI: paste Apex → Transform → enter Name → Approve & Publish
2) (Optional) re-run publish step to refresh External Services actions
3) Invoke:
```bash
curl -s https://apex-zombie-killer.herokuapp.com/ext/Demo/run \
  -H 'Content-Type: application/json' -d '{"payload":{}}'
```
4) In Salesforce: use `HerokuActions.invokeExecuteByName` or Flow action

### 8) Schema (no psql required)
- The app creates tables on startup. If you prefer migrations:
  - Add Flyway and `db/migration/V1__init.sql` or use Spring `schema.sql` with `spring.sql.init.mode=always`

### 9) Notes
- Demo scope: AppLink SSO + CSP; no prod-grade auth/throttling/sandboxing
- Dynamic OpenAPI at `/openapi-generated.yaml` (for automation)
- Default app in examples: `apex-zombie-killer`




