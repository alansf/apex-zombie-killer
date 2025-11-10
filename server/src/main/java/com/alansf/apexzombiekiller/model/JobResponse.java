package com.alansf.apexzombiekiller.model;

import java.time.Instant;
import java.util.UUID;

public class JobResponse {
	public String jobId;
	public String status;
	public Instant startedAt;
	public Instant completedAt;
	public String notes;

	public static JobResponse accepted(String notes) {
		JobResponse r = new JobResponse();
		r.jobId = UUID.randomUUID().toString();
		r.status = "ACCEPTED";
		r.startedAt = Instant.now();
		r.notes = notes;
		return r;
	}
}


