package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.JobRequest;
import com.alansf.apexzombiekiller.model.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountPlanReportingDataJobService {
	private static final Logger log = LoggerFactory.getLogger(AccountPlanReportingDataJobService.class);

	public JobResponse run(JobRequest req) {
		log.info("AccountPlanReportingData: batchSize={}, maxConcurrency={}, dryRun={}",
				req.batchSize, req.maxConcurrency, req.dryRun);
		return JobResponse.accepted("AccountPlanReportingData enqueued");
	}
}


