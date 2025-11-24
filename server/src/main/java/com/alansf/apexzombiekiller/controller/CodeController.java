package com.alansf.apexzombiekiller.controller;

import com.alansf.apexzombiekiller.model.ExecutionAudit;
import com.alansf.apexzombiekiller.model.TransformedCode;
import com.alansf.apexzombiekiller.repo.CodeRepository;
import com.alansf.apexzombiekiller.service.ExecutionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class CodeController {
	private final CodeRepository repo;
	private final ExecutionService exec;

	public CodeController(CodeRepository repo, ExecutionService exec) {
		this.repo = repo;
		this.exec = exec;
	}

	public static class RegisterRequest {
		public String name;
		public String language; // java | js
		public String source;
		public Map<String, Object> metadata;
	}
	public static class RegisterResponse {
		public UUID id;
		public String name;
		public String status;
	}
	public static class ExecuteRequest {
		public Map<String, Object> payload;
	}

	@PostMapping("/code/registrations")
	public RegisterResponse register(@RequestBody RegisterRequest req, @RequestHeader(value="x-sf-user", required=false) String sfUser) {
		String metadataJson = req.metadata == null ? "{}" : com.alansf.apexzombiekiller.service.JsonUtils.toJson(req.metadata);
		TransformedCode code = repo.insertCode(req.name, req.language, req.source, metadataJson, sfUser);
		RegisterResponse r = new RegisterResponse();
		r.id = code.id;
		r.name = code.name;
		r.status = code.status;
		return r;
	}

	@GetMapping("/code/registrations/{id}")
	public TransformedCode get(@PathVariable UUID id) {
		return repo.findById(id).orElseThrow();
	}

	@PostMapping("/code/execute/{id}")
	public ExecutionAudit executeById(@PathVariable UUID id, @RequestBody(required = false) ExecuteRequest req) {
		Map<String, Object> payload = req == null ? Map.of() : Optional.ofNullable(req.payload).orElse(Map.of());
		return exec.executeById(id, payload);
	}

	@PostMapping("/code/execute-by-name/{name}")
	public ExecutionAudit executeByName(@PathVariable String name, @RequestBody(required = false) ExecuteRequest req) {
		Map<String, Object> payload = req == null ? Map.of() : Optional.ofNullable(req.payload).orElse(Map.of());
		return exec.executeByName(name, payload);
	}

	@GetMapping("/code/examples")
	public List<Map<String, String>> examples() {
		List<Map<String, String>> out = new ArrayList<>();
		out.add(example("OppRollup", "java", readResource("/examples/OppRollup.apex")));
		out.add(example("QuoteSync", "java", readResource("/examples/QuoteSync.apex")));
		out.add(example("SplitRollup", "js", readResource("/examples/SplitRollup.apex")));
		return out;
	}

	private static Map<String, String> example(String name, String language, String source) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("name", name);
		m.put("language", language);
		m.put("source", source);
		return m;
	}

	private static String readResource(String path) {
		try {
			var is = CodeController.class.getResourceAsStream(path);
			if (is == null) return "";
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return "";
		}
	}
}


