public with sharing class HerokuActions {
    public class ExecuteByNameInput {
        @InvocableVariable(required=true)
        public String name;
        @InvocableVariable
        public String payloadJson;
    }
    public class ExecuteByNameOutput {
        @InvocableVariable
        public String status;
        @InvocableVariable
        public String responseJson;
    }
    @InvocableMethod(label='Execute Transformed Code by Name')
    public static List<ExecuteByNameOutput> invokeExecuteByName(List<ExecuteByNameInput> inputs) {
        List<ExecuteByNameOutput> results = new List<ExecuteByNameOutput>();
        for (ExecuteByNameInput input : inputs) {
            HttpRequest req = new HttpRequest();
            req.setMethod('POST');
            req.setEndpoint('callout:HerokuJobs/code/execute-by-name/' + EncodingUtil.urlEncode(input.name, 'UTF-8'));
            req.setHeader('Content-Type','application/json');
            String body = input.payloadJson == null ? '{}' : input.payloadJson;
            req.setBody('{"payload":' + body + '}');
            HttpResponse res = new Http().send(req);
            ExecuteByNameOutput out = new ExecuteByNameOutput();
            out.status = String.valueOf(res.getStatusCode());
            out.responseJson = res.getBody();
            results.add(out);
        }
        return results;
    }
}


