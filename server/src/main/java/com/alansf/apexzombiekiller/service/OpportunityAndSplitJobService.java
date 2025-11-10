package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.JobRequest;
import com.alansf.apexzombiekiller.model.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpportunityAndSplitJobService {
	private static final Logger log = LoggerFactory.getLogger(OpportunityAndSplitJobService.class);

	public JobResponse run(JobRequest req) {
		log.info("OpportunityAndSplit: batchSize={}, maxConcurrency={}, dryRun={}",
				req.batchSize, req.maxConcurrency, req.dryRun);
		return JobResponse.accepted("OpportunityAndSplit enqueued");
	}
}


