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

### 6) External Services (optional)
- Setup → External Services → Add Service → `https://apex-zombie-killer-6f48e437a14e.herokuapp.com/openapi.yaml`
- Named Credential: `HerokuJobs`

### 7) Demo flow (end-to-end)
1) Open Salesforce → “Heroku Transformer” page (embedded LWC).
2) Paste Apex (see five snippets below) → select Java or JS → Transform → enter Name → Approve & Publish (updates spec without redeploy).
3) Record‑Triggered Flow on Opportunity (create/update) calls the five job actions:
   - `POST /jobs/product-purchase/run`
   - `POST /jobs/revenue-import/run`
   - `POST /jobs/account-plan-reporting/run`
   - `POST /jobs/cpq-quote-oppty-sync/run`
   - `POST /jobs/opportunity-split/run`
   Optional: add a Decision node (e.g., Stage = Closed Won).
4) Create or update an Opportunity → watch Heroku logs and Flow action responses.
5) Invoke directly (optional):
```bash
HOST=$(heroku apps:info -a apex-zombie-killer | sed -n 's/^Web URL: //p' | tr -d '\r')
curl -s "${HOST}ext/Demo/run" \
  -H 'Content-Type: application/json' -d '{"payload":{}}'
```
6) In Salesforce: use `HerokuActions.invokeExecuteByName('Demo', '{"payload":{}}')` or an External Service action.

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
- Dynamic OpenAPI is regenerated by Approve & Publish; no app redeploy required.




