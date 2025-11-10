package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.JobRequest;
import com.alansf.apexzombiekiller.model.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CPQQuoteOpportunitySyncJobService {
	private static final Logger log = LoggerFactory.getLogger(CPQQuoteOpportunitySyncJobService.class);

	public JobResponse run(JobRequest req) {
		log.info("CPQ_QuoteOpptySync: batchSize={}, maxConcurrency={}, dryRun={}",
				req.batchSize, req.maxConcurrency, req.dryRun);
		return JobResponse.accepted("CPQ_QuoteOpptySync enqueued");
	}
}


