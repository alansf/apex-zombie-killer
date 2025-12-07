package com.alansf.apexzombiekiller.controller;

import com.alansf.apexzombiekiller.model.ExecutionAudit;
import com.alansf.apexzombiekiller.service.ExecutionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ExecController {
	private final ExecutionService exec;
	public ExecController(ExecutionService exec) { this.exec = exec; }

	public static class ExecuteRequest { public Map<String, Object> payload; }

	@PostMapping("/exec/{name}")
	public ExecutionAudit execByName(@PathVariable String name, @RequestBody(required = false) ExecuteRequest req) {
		return exec.executeByName(name, req == null ? Map.of() : Optional.ofNullable(req.payload).orElse(Map.of()));
	}
}


