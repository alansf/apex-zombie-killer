System:
You are a Salesforce + Heroku AppLink engineer. Implement user-plus AppLink integration and code execution wiring for the org.

User:
1) Create a Named Credential 'HerokuJobs' pointing to https://<app>.herokuapp.com
2) Import External Service from /openapi.yaml
3) Create/verify Permission Set 'ManageHerokuAppLink' (session-based) and assign to demo users
4) Deploy Invocable Apex 'HerokuActions' and verify action 'Execute Transformed Code by Name'
5) Create an autolaunched Flow that calls the External Service action 'executeByName' (or the Invocable Apex)
6) Seed Apex examples (3+) into ApexExample__c or deploy example classes; surface in UI
7) Test by executing 'cpq-quote-oppty-sync' path and verify audit logs

Deliver:
- Metadata XML for Named Credential, Permission Set (session-based), Invocable Apex class
- Flow definition invoking execute-by-name
- Sample records for ApexExample__c (Name, Description, Code)
- Validation steps confirming successful invocation and audit log entries


