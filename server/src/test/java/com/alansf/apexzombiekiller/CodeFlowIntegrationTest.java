package com.alansf.apexzombiekiller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CodeFlowIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

	@DynamicPropertySource
	static void dbProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private TestRestTemplate rest;

	@Test
	void approveThenExecuteAndOpenApiIncludesAlias() {
		// 1) Approve simple Java code
		String code =
			"package com.demo.jobs;\n" +
			"public class UserCode { public void run() { } }";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String,Object>> approveReq = new HttpEntity<>(
			Map.of("name","Demo","language","java","source",code, "metadata", Map.of("test", true)),
			headers
		);
		ResponseEntity<String> approveRes = rest.postForEntity("/code/approve", approveReq, String.class);
		Assertions.assertEquals(HttpStatus.OK, approveRes.getStatusCode(), "approve should succeed");
		Assertions.assertTrue(approveRes.getBody() != null && approveRes.getBody().contains("\"name\":\"Demo\""));

		// 2) Execute by alias endpoint
		HttpEntity<Map<String,Object>> execReq = new HttpEntity<>(Map.of("payload", Map.of()), headers);
		ResponseEntity<String> execRes = rest.postForEntity("/ext/Demo/run", execReq, String.class);
		Assertions.assertEquals(HttpStatus.OK, execRes.getStatusCode(), "execute should succeed");
		Assertions.assertTrue(execRes.getBody() != null && execRes.getBody().contains("\"status\":\"SUCCEEDED\""));

		// 3) Dynamic OpenAPI has alias path
		ResponseEntity<String> openapi = rest.getForEntity("/openapi-generated.yaml", String.class);
		Assertions.assertEquals(HttpStatus.OK, openapi.getStatusCode());
		Assertions.assertTrue(openapi.getBody() != null && openapi.getBody().contains("/ext/Demo/run"));
	}
}


