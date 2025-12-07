package com.alansf.apexzombiekiller.model;

import java.time.Instant;
import java.util.UUID;

public class JobItem {
	public UUID id;
	public String jobType; // compile | publish | execute
	public String targetName;
	public String payloadJson;
	public String status; // queued | running | done | error
	public int attempts;
	public Instant enqueuedAt;
	public Instant startedAt;
	public Instant finishedAt;
	public String error;
}


