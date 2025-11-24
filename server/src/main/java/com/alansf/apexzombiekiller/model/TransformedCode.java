package com.alansf.apexzombiekiller.model;

import java.time.Instant;
import java.util.UUID;

public class TransformedCode {
	public UUID id;
	public String name;
	public String language; // "java" | "js"
	public String source;
	public String metadataJson;
	public String status; // READY, DISABLED
	public Instant createdAt;
	public String createdBy;
}


