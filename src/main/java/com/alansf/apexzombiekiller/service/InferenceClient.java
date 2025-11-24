package com.alansf.apexzombiekiller.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class InferenceClient {
	private static final Logger log = LoggerFactory.getLogger(InferenceClient.class);
	private final RestTemplate http = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();

	private Optional<String> endpoint() {
		String url = System.getenv("INFERENCE_ENDPOINT");
		return Optional.ofNullable(url).filter(s -> !s.isBlank());
	}
	private Optional<String> model() {
		String m = System.getenv("INFERENCE_MODEL");
		return Optional.ofNullable(m).filter(s -> !s.isBlank());
	}
	private Optional<String> bearerToken() {
		String t = System.getenv("INFERENCE_TOKEN");
		return Optional.ofNullable(t).filter(s -> !s.isBlank());
	}

	public Optional<String> apexToJava(String apexCode, boolean useHerokuConnect, boolean generateTests) {
		return callTransform("java", apexCode, useHerokuConnect, generateTests);
	}

	public Optional<String> apexToJs(String apexCode, boolean useHerokuConnect, boolean generateTests) {
		return callTransform("js", apexCode, useHerokuConnect, generateTests);
	}

	private Optional<String> callTransform(String target, String apexCode, boolean useHerokuConnect, boolean generateTests) {
		if (endpoint().isEmpty() || model().isEmpty()) {
			return Optional.empty();
		}
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			bearerToken().ifPresent(tok -> headers.set("Authorization", "Bearer " + tok));

			Map<String, Object> body = new HashMap<>();
			body.put("model", model().get());

			String system = "You convert Salesforce Apex to efficient, bulk-safe " +
				(target.equals("java") ? "Java for Spring Boot with Heroku Postgres + Heroku Connect." : "JavaScript for Node/Heroku.") +
				" Replace SOQL/DML-in-loops, use batching, indexed filters, and selective fields. Output only the code.";
			String user = "Options: useHerokuConnect=" + useHerokuConnect + ", generateTests=" + generateTests + "\nApex code:\n" + apexCode;
			Map<String, Object> messages = Map.of(
				"system", system,
				"user", user
			);
			body.put("messages", messages);

			HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
			ResponseEntity<String> res = http.postForEntity(endpoint().get(), req, String.class);
			if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
				log.warn("Inference call failed: status={}, body={}", res.getStatusCode(), res.getBody());
				return Optional.empty();
			}
			JsonNode root = mapper.readTree(res.getBody());
			// Flexible extraction; adapt to actual Managed Inference response schema when available
			if (root.has("output")) {
				return Optional.ofNullable(root.get("output").asText());
			}
			if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
				return Optional.ofNullable(root.get("choices").get(0).path("message").path("content").asText());
			}
			return Optional.of(res.getBody());
		} catch (Exception ex) {
			log.error("Inference unexpected error", ex);
			return Optional.empty();
		}
	}
}



