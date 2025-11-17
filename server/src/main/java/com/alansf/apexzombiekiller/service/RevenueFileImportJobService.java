package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.JobRequest;
import com.alansf.apexzombiekiller.model.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RevenueFileImportJobService {
	private static final Logger log = LoggerFactory.getLogger(RevenueFileImportJobService.class);

	public JobResponse run(JobRequest req) {
		log.info("RevenueFileImport: batchSize={}, maxConcurrency={}, dryRun={}",
				req.batchSize, req.maxConcurrency, req.dryRun);
		return JobResponse.accepted("RevenueFileImport enqueued");
	}
}




