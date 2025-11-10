// Minimal seed for demo; adjust volumes as needed.
List<Account> accounts = new List<Account>();
for (Integer i = 0; i < 200; i++) {
    accounts.add(new Account(Name = 'Demo Account ' + i));
}
insert accounts;

List<Opportunity> opps = new List<Opportunity>();
for (Account a : accounts) {
    for (Integer j = 0; j < 3; j++) {
        opps.add(new Opportunity(
            Name = 'Opp ' + a.Name + ' #' + j,
            StageName = 'Prospecting',
            CloseDate = System.today().addDays(30),
            AccountId = a.Id,
            Amount = 1000
        ));
    }
}
insert opps;


