package com.alansf.apexzombiekiller.service;

import com.alansf.apexzombiekiller.model.TransformRequest;
import com.alansf.apexzombiekiller.model.TransformResponse;
import org.springframework.stereotype.Service;

@Service
public class TransformService {
	private final InferenceClient inference;
	public TransformService(InferenceClient inference) {
		this.inference = inference;
	}
	public TransformResponse apexToJava(TransformRequest req) {
		TransformResponse res = new TransformResponse();
		// Prefer Managed Inference when configured; otherwise fallback stub
		String body = req.apexCode == null ? "" : req.apexCode;
		boolean useConnect = req.options != null && Boolean.TRUE.equals(req.options.useHerokuConnect);
		boolean genTests = req.options != null && Boolean.TRUE.equals(req.options.generateTests);
		var maybe = inference.apexToJava(body, useConnect, genTests);
		if (maybe.isPresent() && maybe.get() != null && !maybe.get().isBlank()) {
			res.javaCode = maybe.get();
			res.notes = "Generated via Managed Inference";
		} else {
			res.javaCode =
					"package com.demo.jobs;\n\n" +
					"public class ConvertedFromApex {\n" +
					"    public void run() {\n" +
					"        /*\n" + body + "\n" +
					"        */\n" +
					"    }\n" +
					"}\n";
			res.testCode =
					"package com.demo.jobs;\n\n" +
					"import org.junit.jupiter.api.Test;\n" +
					"import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;\n\n" +
					"public class ConvertedFromApexTest {\n" +
					"    @Test\n" +
					"    void executes() {\n" +
					"        assertDoesNotThrow(() -> new ConvertedFromApex().run());\n" +
					"    }\n" +
					"}\n";
			res.notes = "Fallback stub. Configure INFERENCE_URL and INFERENCE_MODEL_ID for Managed Inference.";
		}
		return res;
	}

	public TransformResponse apexToJs(TransformRequest req) {
		TransformResponse res = new TransformResponse();
		String body = req.apexCode == null ? "" : req.apexCode;
		boolean useConnect = req.options != null && Boolean.TRUE.equals(req.options.useHerokuConnect);
		boolean genTests = req.options != null && Boolean.TRUE.equals(req.options.generateTests);
		var maybe = inference.apexToJs(body, useConnect, genTests);
		if (maybe.isPresent() && maybe.get() != null && !maybe.get().isBlank()) {
			res.jsCode = maybe.get();
			res.notes = "Generated via Managed Inference";
		} else {
			res.jsCode =
					"export function convertedFromApex() {\n" +
					"  /*\n" + body + "\n" +
					"  */\n" +
					"}\n";
			res.notes = "Fallback stub. Configure INFERENCE_URL and INFERENCE_MODEL_ID for Managed Inference.";
		}
		return res;
	}
}




