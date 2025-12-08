package com.alansf.apexzombiekiller.controller;

import com.alansf.apexzombiekiller.model.ExecutionAudit;
import com.alansf.apexzombiekiller.model.TransformedCode;
import com.alansf.apexzombiekiller.repo.CodeRepository;
import com.alansf.apexzombiekiller.service.ExecutionService;
import com.alansf.apexzombiekiller.service.PublishService;
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
	private final PublishService publish;

	public CodeController(CodeRepository repo, ExecutionService exec, PublishService publish) {
		this.repo = repo;
		this.exec = exec;
		this.publish = publish;
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
	public static class ApproveRequest {
		public String name;
		public String language;
		public String source;
		public Map<String, Object> metadata;
	}
	public static class ApproveResponse {
		public UUID id;
		public String name;
		public String status;
		public String notes;
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

	@PostMapping("/code/approve")
	public ApproveResponse approve(@RequestBody ApproveRequest req, @RequestHeader(value="x-sf-user", required=false) String sfUser) {
		if (req == null || req.name == null || req.language == null || req.source == null) {
			throw new IllegalArgumentException("name, language, and source are required");
		}
		// Smoke test compile/exec (Java only in demo; JS engine may be absent)
		try {
			if ("java".equalsIgnoreCase(req.language)) {
				new com.alansf.apexzombiekiller.service.JavaExecutionAdapter().execute("com.demo.jobs.UserCode", req.source);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Compilation/runtime check failed: " + e.getMessage());
		}
		String metadataJson = req.metadata == null ? "{}" : com.alansf.apexzombiekiller.service.JsonUtils.toJson(req.metadata);
		TransformedCode code = repo.upsertCode(req.name, req.language, req.source, metadataJson, sfUser);
		ApproveResponse r = new ApproveResponse();
		r.id = code.id;
		r.name = code.name;
		r.status = code.status;
		// Default binding + publish job
		try {
			// Upsert default web binding and enqueue compile+publish jobs
			var app = org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
			var bindingRepo = app.getBean(com.alansf.apexzombiekiller.repo.BindingRepository.class);
			var jobRepo = app.getBean(com.alansf.apexzombiekiller.repo.JobQueueRepository.class);
			bindingRepo.upsertWeb(code.name, "/exec/" + code.name);
			jobRepo.enqueue("compile", code.name, null);
			jobRepo.enqueue("publish", code.name, null);
		} catch (Exception ignore) {
			// Non-fatal in demo
		}
		String jobId = publish.queueRepublish();
		r.notes = "Approved and ready. Use /exec/" + code.name + " or /ext/" + code.name + "/run. Publish job=" + jobId;
		return r;
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

	@PostMapping("/ext/{name}/run")
	public ExecutionAudit extExecute(@PathVariable String name, @RequestBody(required = false) ExecuteRequest req) {
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


