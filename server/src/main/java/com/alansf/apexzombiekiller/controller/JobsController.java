package com.alansf.apexzombiekiller.controller;

import com.alansf.apexzombiekiller.model.JobRequest;
import com.alansf.apexzombiekiller.model.JobResponse;
import com.alansf.apexzombiekiller.service.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class JobsController {
	private final ProductPurchaseProcessJobService productPurchase;
	private final RevenueFileImportJobService revenueImport;
	private final AccountPlanReportingDataJobService accountPlan;
	private final CPQQuoteOpportunitySyncJobService cpqSync;
	private final OpportunityAndSplitJobService oppSplit;

	public JobsController(ProductPurchaseProcessJobService productPurchase,
	                      RevenueFileImportJobService revenueImport,
	                      AccountPlanReportingDataJobService accountPlan,
	                      CPQQuoteOpportunitySyncJobService cpqSync,
	                      OpportunityAndSplitJobService oppSplit) {
		this.productPurchase = productPurchase;
		this.revenueImport = revenueImport;
		this.accountPlan = accountPlan;
		this.cpqSync = cpqSync;
		this.oppSplit = oppSplit;
	}

	@PostMapping("/product-purchase/run")
	public JobResponse runProductPurchase(@RequestBody(required = false) JobRequest req) {
		return productPurchase.run(nullSafe(req));
	}

	@PostMapping("/revenue-file-import/run")
	public JobResponse runRevenueImport(@RequestBody(required = false) JobRequest req) {
		return revenueImport.run(nullSafe(req));
	}

	@PostMapping("/account-plan-reporting/run")
	public JobResponse runAccountPlan(@RequestBody(required = false) JobRequest req) {
		return accountPlan.run(nullSafe(req));
	}

	@PostMapping("/cpq-quote-oppty-sync/run")
	public JobResponse runCpqSync(@RequestBody(required = false) JobRequest req) {
		return cpqSync.run(nullSafe(req));
	}

	@PostMapping("/opportunity-split/run")
	public JobResponse runOppSplit(@RequestBody(required = false) JobRequest req) {
		return oppSplit.run(nullSafe(req));
	}

	private static JobRequest nullSafe(JobRequest req) {
		return req == null ? new JobRequest() : req;
	}
}




