package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.TransformedCode;
import com.alansf.apexzombiekiller.repo.CodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenApiService {
	private final CodeRepository repo;
	public OpenApiService(CodeRepository repo) { this.repo = repo; }

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
			+ "  /code/execute-by-name/{name}:\n"
			+ "    post:\n"
			+ "      operationId: executeByName\n"
			+ "      parameters:\n"
			+ "        - in: path\n"
			+ "          name: name\n"
			+ "          required: true\n"
			+ "          schema: { type: string }\n"
			+ "      requestBody:\n"
			+ "        required: false\n"
			+ "        content:\n"
			+ "          application/json:\n"
			+ "            schema:\n"
			+ "              type: object\n"
			+ "              properties:\n"
			+ "                payload: { type: object }\n"
			+ "      responses:\n"
			+ "        \"200\":\n"
			+ "          description: OK\n"
			+ "          content:\n"
			+ "            application/json:\n"
			+ "              schema:\n"
			+ "                $ref: '#/components/schemas/ExecutionAudit'\n"
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
}


