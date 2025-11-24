package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.ExecutionAudit;
import com.alansf.apexzombiekiller.model.TransformedCode;
import com.alansf.apexzombiekiller.repo.CodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExecutionService {
	private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);
	private final CodeRepository repo;
	private final JavaExecutionAdapter javaAdapter = new JavaExecutionAdapter();
	private final JsExecutionAdapter jsAdapter = new JsExecutionAdapter();

	public ExecutionService(CodeRepository repo) {
		this.repo = repo;
	}

	public ExecutionAudit executeById(UUID id, Map<String, Object> payload) {
		Optional<TransformedCode> opt = repo.findById(id);
		if (opt.isEmpty()) {
			throw new IllegalArgumentException("Code not found: " + id);
		}
		TransformedCode code = opt.get();
		return execute(code, payload);
	}

	public ExecutionAudit executeByName(String name, Map<String, Object> payload) {
		Optional<TransformedCode> opt = repo.findByName(name);
		if (opt.isEmpty()) {
			throw new IllegalArgumentException("Code not found: " + name);
		}
		TransformedCode code = opt.get();
		return execute(code, payload);
	}

	private ExecutionAudit execute(TransformedCode code, Map<String, Object> payload) {
		ExecutionAudit audit = repo.createAudit(code.id, code.name, JsonUtils.toJson(payload));
		repo.markAuditRunning(audit.id);
		try {
			if ("java".equalsIgnoreCase(code.language)) {
				// Expect a class with no-arg constructor and a public void run() method.
				javaAdapter.execute("com.demo.jobs.UserCode", code.source);
			} else if ("js".equalsIgnoreCase(code.language)) {
				jsAdapter.execute(code.source);
			} else {
				throw new IllegalArgumentException("Unsupported language: " + code.language);
			}
			repo.markAuditSuccess(audit.id);
		} catch (Exception ex) {
			log.error("Execution failed for {}", code.name, ex);
			repo.markAuditFailure(audit.id, ex.getMessage());
		}
		return repo.findAudit(audit.id).orElse(audit);
	}
}


