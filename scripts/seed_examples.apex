List<ApexExample__c> rows = new List<ApexExample__c>();
rows.add(new ApexExample__c(Name__c='OppRollup', Description__c='Roll up opp totals by account', Code__c='public class OppRollup { public static void run(){} }'));
rows.add(new ApexExample__c(Name__c='QuoteSync', Description__c='Sync quotes with OLI', Code__c='public class QuoteSync { public static void run(){} }'));
rows.add(new ApexExample__c(Name__c='SplitRollup', Description__c='Recalculate splits', Code__c='public class SplitRollup { public static void run(){} }'));
insert rows;


