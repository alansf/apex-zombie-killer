package com.alansf.apexzombiekiller.controller;

import com.alansf.apexzombiekiller.model.TransformRequest;
import com.alansf.apexzombiekiller.model.TransformResponse;
import com.alansf.apexzombiekiller.service.OpenApiService;
import com.alansf.apexzombiekiller.service.TransformService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TransformController {
	private final TransformService transformService;
	private final OpenApiService openApiService;

	public TransformController(TransformService transformService, OpenApiService openApiService) {
		this.transformService = transformService;
		this.openApiService = openApiService;
	}

	@PostMapping("/transform/apex-to-java")
	public TransformResponse apexToJava(@RequestBody TransformRequest req) {
		return transformService.apexToJava(req);
	}

	@PostMapping("/transform/apex-to-js")
	public TransformResponse apexToJs(@RequestBody TransformRequest req) {
		return transformService.apexToJs(req);
	}

	@GetMapping(value = "/openapi-generated.yaml", produces = "application/yaml")
	public String generatedOpenApi() {
		// Best-effort derive server URL from env or request-origin; here use env var or fall back to placeholder
		String url = System.getenv().getOrDefault("APP_BASE_URL", "https://apex-zombie-killer-6f48e437a14e.herokuapp.com");
		return openApiService.generateYaml(url);
	}
}




