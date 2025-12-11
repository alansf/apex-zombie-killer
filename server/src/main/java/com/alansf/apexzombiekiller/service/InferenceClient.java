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
			log.warn("Inference not configured: endpoint={}, model={}", endpoint().orElse("missing"), model().orElse("missing"));
			return Optional.empty();
		}
		// Build headers once
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		bearerToken().ifPresent(tok -> headers.set("Authorization", "Bearer " + tok));
		log.debug("Calling inference: endpoint={}, model={}", endpoint().get(), model().get());

		// Construct a flexible body that can satisfy common schemas
		Map<String, Object> body = new HashMap<>();
		body.put("model", model().get());

		String system = "You convert Salesforce Apex to efficient, bulk-safe " +
			(target.equals("java") 
				? "standalone executable Java code. Generate a single public class with a public void run() method (or public static void main(String[] args)). Use standard Java libraries only - no Spring annotations (@Service, @Autowired, etc.). The code should be self-contained and executable without framework dependencies. Replace SOQL/DML-in-loops with batch processing, use indexed filters, and selective fields."
				: "JavaScript for Node/Heroku.") +
			" Output only the code, no explanations.";
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
					StringBuilder contentAccumulator = new StringBuilder();
					try {
						log.debug("Attempting streaming request to: {}", url);
						http.post()
								.uri(url)
								.headers(h -> {
									headers.forEach((k, v) -> h.addAll(k, v));
									h.set("Accept", "text/event-stream"); // SSE format
								})
								.bodyValue(payload)
								.retrieve()
								.bodyToFlux(String.class)
								.doOnNext(chunk -> {
									// Handle SSE format: "data: {...}\n\n" or plain JSON chunks
									if (chunk != null && !chunk.isBlank()) {
										String[] lines = chunk.split("\n");
										for (String line : lines) {
											line = line.trim();
											if (line.startsWith("data: ")) {
												line = line.substring(6); // Remove "data: " prefix
											}
											if (line.equals("[DONE]") || line.isEmpty()) {
												continue;
											}
											try {
												JsonNode node = mapper.readTree(line);
												// Extract content from delta or message
												if (node.has("choices") && node.get("choices").isArray() && node.get("choices").size() > 0) {
													JsonNode choice = node.get("choices").get(0);
													if (choice.has("delta") && choice.get("delta").has("content")) {
														String deltaContent = choice.get("delta").get("content").asText();
														if (deltaContent != null && !deltaContent.isBlank()) {
															contentAccumulator.append(deltaContent);
														}
													} else if (choice.has("message") && choice.get("message").has("content")) {
														String msgContent = choice.get("message").get("content").asText();
														if (msgContent != null && !msgContent.isBlank()) {
															contentAccumulator.append(msgContent);
														}
													}
												}
											} catch (Exception e) {
												// Not JSON, might be partial chunk - accumulate as-is
												if (!line.startsWith("data:") && !line.equals("[DONE]")) {
													contentAccumulator.append(line);
												}
											}
										}
									}
								})
								.doOnError(err -> log.warn("Streaming error for url={}, error={}", url, err.getMessage(), err))
								.retryWhen(Retry.max(1))
								.blockLast();
						log.debug("Streaming completed, accumulated {} chars", contentAccumulator.length());
					} catch (Exception ex) {
						log.error("Streaming request failed for url={}, error={}", url, ex.getMessage(), ex);
					}
					if (contentAccumulator.length() > 0) {
						log.info("Successfully extracted {} chars from inference stream", contentAccumulator.length());
						return Optional.of(contentAccumulator.toString());
					} else {
						log.warn("No content extracted from inference stream for url={}, trying non-streaming fallback", url);
						// Fallback to non-streaming request
						try {
							Map<String, Object> nonStreamPayload = new HashMap<>(payload);
							nonStreamPayload.remove("stream"); // Remove stream flag
							String res = http.post()
									.uri(url)
									.headers(h -> headers.forEach((k, v) -> h.addAll(k, v)))
									.bodyValue(nonStreamPayload)
									.retrieve()
									.bodyToMono(String.class)
									.retryWhen(Retry.max(1))
									.block();
							if (res != null && !res.isBlank()) {
								JsonNode root = mapper.readTree(res);
								if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
									String content = root.get("choices").get(0).path("message").path("content").asText();
									if (content != null && !content.isBlank()) {
										log.info("Successfully got content from non-streaming response");
										return Optional.of(content);
									}
								}
							}
						} catch (Exception fallbackEx) {
							log.warn("Non-streaming fallback also failed for url={}", url, fallbackEx);
						}
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
				log.error("Inference unexpected error for url={}, error={}", url, ex.getMessage(), ex);
				// Move on to next candidate
			}
		}
		log.warn("All inference URL candidates failed. Check INFERENCE_URL, INFERENCE_MODEL_ID, and INFERENCE_KEY.");
		return Optional.empty();
	}

	private static String trimTrailingSlash(String s) {
		if (s == null || s.isEmpty()) return s;
		return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
	}
}



