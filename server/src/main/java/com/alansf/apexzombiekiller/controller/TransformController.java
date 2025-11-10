package com.alansf.apexzombiekiller.controller;

import com.alansf.apexzombiekiller.model.TransformRequest;
import com.alansf.apexzombiekiller.model.TransformResponse;
import com.alansf.apexzombiekiller.service.TransformService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class TransformController {
	private final TransformService transformService;

	public TransformController(TransformService transformService) {
		this.transformService = transformService;
	}

	@PostMapping("/transform/apex-to-java")
	public TransformResponse apexToJava(@RequestBody TransformRequest req) {
		return transformService.apexToJava(req);
	}

	@PostMapping("/transform/apex-to-js")
	public TransformResponse apexToJs(@RequestBody TransformRequest req) {
		return transformService.apexToJs(req);
	}
}


