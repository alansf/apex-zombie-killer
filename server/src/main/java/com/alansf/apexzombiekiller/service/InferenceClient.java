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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InferenceClient {
	private static final Logger log = LoggerFactory.getLogger(InferenceClient.class);
	private final WebClient http = WebClient.builder().build();
	private final ObjectMapper mapper = new ObjectMapper();

	private Optional<String> endpoint() {
		// Use INFERENCE_URL only
		String url = System.getenv("INFERENCE_URL");
		return Optional.ofNullable(url).filter(s -> s != null && !s.isBlank());
	}
	private Optional<String> model() {
		// Use INFERENCE_MODEL_ID only
		String m = System.getenv("INFERENCE_MODEL_ID");
		return Optional.ofNullable(m).filter(s -> s != null && !s.isBlank());
	}
	private Optional<String> bearerToken() {
		// Prefer demo env naming (INFERENCE_KEY), fallback to legacy INFERENCE_TOKEN
		String t = System.getenv("INFERENCE_KEY");
		if (t == null || t.isBlank()) {
			t = System.getenv("INFERENCE_TOKEN");
		}
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
		// Build headers once
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		bearerToken().ifPresent(tok -> headers.set("Authorization", "Bearer " + tok));

		// Construct a flexible body that can satisfy common schemas
		Map<String, Object> body = new HashMap<>();
		body.put("model", model().get());

		String system = "You convert Salesforce Apex to efficient, bulk-safe " +
			(target.equals("java") ? "Java for Spring Boot with Heroku Postgres + Heroku Connect." : "JavaScript for Node/Heroku.") +
			" Replace SOQL/DML-in-loops, use batching, indexed filters, and selective fields. Output only the code.";
		String user = "Options: useHerokuConnect=" + useHerokuConnect + ", generateTests=" + generateTests + "\nApex code:\n" + apexCode;
		// OpenAI/Anthropic-style messages (array)
		body.put("messages", List.of(
			Map.of("role", "system", "content", system),
			Map.of("role", "user", "content", user)
		));
		// Generic single-field prompt for simpler /v1/infer style APIs
		body.put("input", system + "\n\n" + user);

		// Prepare candidate URLs:
		// - If the configured endpoint already has a versioned path, try it first then a couple of alternates
		// - If it's just a host/base, append known paths
		String configured = endpoint().get().trim();
		boolean hasVersionPath = configured.matches(".*/v\\d+/.*");
		List<String> candidates = hasVersionPath
			? List.of(
				configured,
				configured.replace("/v1/infer", "/v1/messages"),
				configured.replace("/v1/messages", "/v1/infer"),
				configured.replace("/v1/messages", "/v1/chat/completions")
			)
			: List.of(
				// Prefer chat completions as per current docs
				trimTrailingSlash(configured) + "/v1/chat/completions",
				trimTrailingSlash(configured) + "/v1/messages",
				trimTrailingSlash(configured) + "/v1/infer"
			);

		for (String url : candidates) {
			try {
				// Build request body per target schema to avoid 400s on unknown args
				Map<String, Object> payload = new HashMap<>();
				payload.put("model", model().get());
				if (url.contains("/v1/chat/completions")) {
					// Strict chat schema: messages only
					payload.put("messages", List.of(
						Map.of("role", "system", "content", system),
						Map.of("role", "user", "content", user)
					));
					payload.put("stream", true);
				} else if (url.contains("/v1/infer")) {
					// Simple infer schema: input only
					payload.put("input", system + "\n\n" + user);
				} else {
					// Generic messages endpoint
					payload.put("messages", List.of(
						Map.of("role", "user", "content", system + "\n\n" + user)
					));
				}

				// Streaming for chat completions; fall back to non-stream for others
				if (url.contains("/v1/chat/completions")) {
					StringBuilder acc = new StringBuilder();
					Flux<String> flux = http.post()
							.uri(url)
							.headers(h -> {
								headers.forEach((k, v) -> h.addAll(k, v));
							})
							.bodyValue(payload)
							.retrieve()
							.bodyToFlux(String.class)
							.retryWhen(Retry.max(1));
					flux.blockLast(s -> {
						try {
							// Some providers stream JSON lines; append raw for demo
							acc.append(s);
						} catch (Exception ignore) {}
					});
					if (acc.length() > 0) {
						try {
							JsonNode root = mapper.readTree(acc.toString());
							if (root.has("choices")) {
								return Optional.ofNullable(root.get("choices").get(0).path("message").path("content").asText());
							}
						} catch (Exception ignore) {}
						return Optional.of(acc.toString());
					}
				} else {
					String res = http.post()
							.uri(url)
							.headers(h -> headers.forEach((k, v) -> h.addAll(k, v)))
							.bodyValue(payload)
							.retrieve()
							.bodyToMono(String.class)
							.retryWhen(Retry.max(1))
							.block();
					if (res != null) {
						JsonNode root = mapper.readTree(res);
						if (root.has("output")) return Optional.ofNullable(root.get("output").asText());
						if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
							return Optional.ofNullable(root.get("choices").get(0).path("message").path("content").asText());
						}
						return Optional.of(res);
					}
				}
			} catch (HttpClientErrorException.NotFound nf) {
				log.warn("Inference path 404, trying next candidate: {}", url);
			} catch (Exception ex) {
				log.error("Inference unexpected error for url={}", url, ex);
				// Move on to next candidate
			}
		}
		return Optional.empty();
	}

	private static String trimTrailingSlash(String s) {
		if (s == null || s.isEmpty()) return s;
		return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
	}
}



