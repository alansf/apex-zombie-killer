package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.TransformedCode;
import com.alansf.apexzombiekiller.repo.CodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenApiService {
	private final CodeRepository repo;
	private final com.alansf.apexzombiekiller.repo.BindingRepository bindings;
	public OpenApiService(CodeRepository repo, com.alansf.apexzombiekiller.repo.BindingRepository bindings) {
		this.repo = repo;
		this.bindings = bindings;
	}

	public String generateYaml(String serverUrl) {
		List<TransformedCode> codes = repo.listAll();
		String aliasPaths = codes.stream()
				.map(c -> "  /ext/" + c.name + "/run:\n" +
						"    post:\n" +
						"      operationId: extRun_" + c.name + "\n" +
						"      requestBody:\n" +
						"        required: false\n" +
						"        content:\n" +
						"          application/json:\n" +
						"            schema:\n" +
						"              type: object\n" +
						"              properties:\n" +
						"                payload: { type: object }\n" +
						"      responses:\n" +
						"        \"200\":\n" +
						"          description: OK\n" +
						"          content:\n" +
						"            application/json:\n" +
						"              schema:\n" +
						"                $ref: '#/components/schemas/ExecutionAudit'\n")
				.collect(Collectors.joining());
		return ""
			+ "openapi: 3.0.3\n"
			+ "info:\n"
			+ "  title: Apex Performance Offload API (Dynamic)\n"
			+ "  version: \"1.0.0\"\n"
			+ "servers:\n"
			+ "  - url: " + serverUrl + "\n"
			+ "paths:\n"
			+ dynamicWebPaths()
			+ aliasPaths
			+ "components:\n"
			+ "  schemas:\n"
			+ "    ExecutionAudit:\n"
			+ "      type: object\n"
			+ "      properties:\n"
			+ "        id: { type: string, format: uuid }\n"
			+ "        codeId: { type: string, format: uuid }\n"
			+ "        codeName: { type: string }\n"
			+ "        startedAt: { type: string, format: date-time }\n"
			+ "        completedAt: { type: string, format: date-time, nullable: true }\n"
			+ "        status: { type: string }\n"
			+ "        error: { type: string }\n"
			+ "        inputJson: { type: string }\n";
	}

	private String dynamicWebPaths() {
		var list = bindings.listWeb();
		StringBuilder sb = new StringBuilder();
		for (var b : list) {
			// Default path: /exec/{name}
			String path = "/exec/" + b.name;
			try {
				var cfg = b.configJson == null ? "{}" : b.configJson;
				if (cfg.contains("\"path\"")) {
					int i = cfg.indexOf("\"path\"");
					int q = cfg.indexOf('"', i + 6);
					int q2 = cfg.indexOf('"', q + 1);
					if (q > 0 && q2 > q) path = cfg.substring(q + 1, q2);
				}
			} catch (Exception ignored) {}
			sb.append("  ").append(path).append(":\n")
					.append("    post:\n")
					.append("      operationId: exec_").append(b.name).append("\n")
					.append("      requestBody:\n")
					.append("        required: false\n")
					.append("        content:\n")
					.append("          application/json:\n")
					.append("            schema:\n")
					.append("              type: object\n")
					.append("              properties:\n")
					.append("                payload: { type: object }\n")
					.append("      responses:\n")
					.append("        \"200\":\n")
					.append("          description: OK\n")
					.append("          content:\n")
					.append("            application/json:\n")
					.append("              schema:\n")
					.append("                $ref: '#/components/schemas/ExecutionAudit'\n");
		}
		return sb.toString();
	}
}


