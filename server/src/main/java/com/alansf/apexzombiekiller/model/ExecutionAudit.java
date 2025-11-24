package com.alansf.apexzombiekiller.model;

import java.time.Instant;
import java.util.UUID;

public class ExecutionAudit {
	public UUID id;
	public UUID codeId;
	public String codeName;
	public Instant startedAt;
	public Instant completedAt;
	public String status; // ACCEPTED, RUNNING, SUCCEEDED, FAILED
	public String error;
	public String inputJson;
}


