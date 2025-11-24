package com.alansf.apexzombiekiller.service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

public class JsExecutionAdapter {
	public void execute(String source) throws Exception {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("graal.js");
		if (engine == null) {
			throw new IllegalStateException("GraalJS engine not available. Ensure Graal JS dependency is present.");
		}
		engine.eval(source, new SimpleBindings());
	}
}


