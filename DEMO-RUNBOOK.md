## Demo Runbook - AppLink (user-plus) Web Dyno + Code Execution

1) Heroku (app = apex-zombie-killer-6f48e437a14e)
- heroku apps:info -a apex-zombie-killer-6f48e437a14e
- heroku buildpacks:add heroku/java -a apex-zombie-killer-6f48e437a14e
- (if required by AppLink) add service mesh buildpack before java
- heroku addons:create heroku-postgresql:standard-0 -a apex-zombie-killer-6f48e437a14e
- heroku plugins:install heroku-connect
- heroku addons:create herokuconnect:demo -a apex-zombie-killer-6f48e437a14e
- heroku connect:import heroku/connect-mapping.json -a apex-zombie-killer-6f48e437a14e
- Deploy server (subtree): git subtree push --prefix server heroku main

### Managed Inference (MCP) env
- heroku addons:create <managed-inference-addon> -a apex-zombie-killer-6f48e437a14e
- heroku config:set INFERENCE_MODEL=\"anthropic/claude-3.5-sonnet\" -a apex-zombie-killer-6f48e437a14e
- heroku config:set INFERENCE_ENDPOINT=\"https://inference.herokuapi.com\" -a apex-zombie-killer-6f48e437a14e

2) AppLink publish (user-plus)
- heroku salesforce:publish apispec.yaml \
  --client-name HerokuAPI \
  --connection-name purple-zombie \
  --authorization-connected-app-name "MyAppLinkApp" \
  --authorization-permission-set-name "ManageHerokuAppLink" \
  -a apex-zombie-killer-6f48e437a14e

3) Salesforce org
- sf org login web --set-default-dev-hub --alias DevHub
- sf org create scratch --definition-file config/project-scratch-def.json --set-default --alias apex-perf-demo
- sf project deploy start --source-dir force-app --target-org apex-perf-demo
- (Optional) sf apex run --file scripts/seed_examples.apex --target-org apex-perf-demo
- Setup → External Services → Add Service → https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi.yaml
- Setup → Heroku → Link apex-zombie-killer-6f48e437a14e

4) Use the UI
- Open AppLink UI (SSO)
- Paste Apex → transform → register → execute-by-name
- Observe logs in Heroku and audit rows (execution_audit)

5) Flow / Apex
- Create a Flow that calls External Service executeByName OR
- Use Invocable Apex 'HerokuActions.invokeExecuteByName'

6) Verify
- curl -s https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi.yaml | head
- curl -s -X POST https://apex-zombie-killer-6f48e437a14e.herokuapp.com/code/registrations -H 'Content-Type: application/json' -d '{"name":"Demo","language":"java","source":"public class UserCode{ public void run(){ System.out.println(\"hi\"); } }"}'
- curl -s -X POST https://apex-zombie-killer-6f48e437a14e.herokuapp.com/code/execute-by-name/Demo -H 'Content-Type: application/json' -d '{"payload":{}}'


