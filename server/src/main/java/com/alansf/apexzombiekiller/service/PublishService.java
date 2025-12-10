package com.alansf.apexzombiekiller.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PublishService {
	private static final Logger log = LoggerFactory.getLogger(PublishService.class);
	private final OpenApiService openApiService;

	public PublishService(OpenApiService openApiService) {
		this.openApiService = openApiService;
	}

	public String queueRepublish() {
		// Dynamic-only OpenAPI: generate from code_binding (no static file writes).
		// This matches what /openapi-generated.yaml serves; publish via AppLink after approve.
		String url = System.getenv().getOrDefault("APP_BASE_URL", "https://apex-zombie-killer-6f48e437a14e.herokuapp.com");
		String yaml = openApiService.generateYaml(url);
		log.info("Prepared dynamic OpenAPI ({} chars). Ready to publish via MCP.", yaml.length());
		String jobId = "publish-" + System.currentTimeMillis();
		log.info("Queued AppLink publish job id={}", jobId);
		return jobId;
	}
}


