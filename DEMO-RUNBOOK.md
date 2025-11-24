## Demo Runbook - AppLink (user-plus) Web Dyno + Code Execution

1) Heroku
- heroku apps:create <app>
- heroku buildpacks:add heroku/java -a <app>
- (if required by AppLink) add service mesh buildpack before java
- heroku addons:create heroku-postgresql:standard-0 -a <app>
- heroku plugins:install heroku-connect
- heroku addons:create herokuconnect:demo -a <app>
- heroku connect:import heroku/connect-mapping.json -a <app>
- Deploy server (subtree): git subtree push --prefix server heroku main

### Managed Inference (MCP) env
- heroku addons:create <managed-inference-addon> -a <app>
- heroku config:set INFERENCE_MODEL=\"anthropic/claude-3.5-sonnet\" -a <app>
- heroku config:set INFERENCE_ENDPOINT=\"https://inference.herokuapi.com\" -a <app>

2) AppLink publish (user-plus)
- heroku salesforce:publish apispec.yaml \
  --client-name HerokuAPI \
  --connection-name purple-zombie \
  --authorization-connected-app-name "MyAppLinkApp" \
  --authorization-permission-set-name "ManageHerokuAppLink" \
  -a <app>

3) Salesforce org
- sf org login web --set-default-dev-hub --alias DevHub
- sf org create scratch --definition-file config/project-scratch-def.json --set-default --alias apex-perf-demo
- sf project deploy start --source-dir force-app --target-org apex-perf-demo
- (Optional) sf apex run --file scripts/seed_examples.apex --target-org apex-perf-demo
- Setup → External Services → Add Service → https://<app>.herokuapp.com/openapi.yaml
- Setup → Heroku → Link <app>

4) Use the UI
- Open AppLink UI (SSO)
- Paste Apex → transform → register → execute-by-name
- Observe logs in Heroku and audit rows (execution_audit)

5) Flow / Apex
- Create a Flow that calls External Service executeByName OR
- Use Invocable Apex 'HerokuActions.invokeExecuteByName'

6) Verify
- curl -s https://<app>.herokuapp.com/openapi.yaml | head
- curl -s -X POST https://<app>.herokuapp.com/code/registrations -H 'Content-Type: application/json' -d '{"name":"Demo","language":"java","source":"public class UserCode{ public void run(){ System.out.println(\"hi\"); } }"}'
- curl -s -X POST https://<app>.herokuapp.com/code/execute-by-name/Demo -H 'Content-Type: application/json' -d '{"payload":{}}'


