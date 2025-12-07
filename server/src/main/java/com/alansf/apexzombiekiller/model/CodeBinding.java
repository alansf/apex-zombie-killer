package com.alansf.apexzombiekiller.model;

import java.time.Instant;
import java.util.UUID;

public class CodeBinding {
	public UUID id;
	public String name;
	public String type; // web | trigger | queue
	public String configJson;
	public Instant createdAt;
	public Instant updatedAt;
}


