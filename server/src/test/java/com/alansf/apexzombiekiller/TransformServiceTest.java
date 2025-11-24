package com.alansf.apexzombiekiller;

import com.alansf.apexzombiekiller.model.TransformRequest;
import com.alansf.apexzombiekiller.service.InferenceClient;
import com.alansf.apexzombiekiller.service.TransformService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransformServiceTest {

	@Test
	void fallsBackWhenNoInferenceConfigured() {
		TransformService svc = new TransformService(new InferenceClient());
		TransformRequest req = new TransformRequest();
		req.apexCode = "global class X {}";
		TransformRequest.Options opts = new TransformRequest.Options();
		opts.useHerokuConnect = true;
		opts.generateTests = true;
		req.options = opts;

		var res = svc.apexToJava(req);
		assertNotNull(res.javaCode);
		assertTrue(res.javaCode.contains("public class ConvertedFromApex"));
		assertTrue(res.notes != null);
	}
}


