package com.alansf.apexzombiekiller.service;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaExecutionAdapter {
	private static final Map<String, Class<?>> cache = new ConcurrentHashMap<>();

	public void execute(String className, String source) throws Exception {
		// Strip any package declaration to simplify in-memory compilation and loading.
		String sanitized = stripPackage(source);
		// Auto-detect the primary class name (prefer public), fallback to provided name.
		String effectiveName = detectClassName(sanitized);
		if (effectiveName == null || effectiveName.isBlank()) {
			effectiveName = className;
		}
		final String finalName = effectiveName;
		final String finalSource = sanitized;
		Class<?> clazz = cache.computeIfAbsent(finalName, n -> compile(finalName, finalSource));
		Object instance = clazz.getDeclaredConstructor().newInstance();
		clazz.getMethod("run").invoke(instance);
	}

	private Class<?> compile(String className, String source) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		JavaFileObject file = new JavaSourceFromString(className, source);
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		InMemoryClassFileManager memManager = new InMemoryClassFileManager(fileManager);
		JavaCompiler.CompilationTask task = compiler.getTask(null, memManager, diagnostics,
				Arrays.asList("-target", "21", "-source", "21"), null, Collections.singletonList(file));
		Boolean ok = task.call();
		if (!Boolean.TRUE.equals(ok)) {
			StringBuilder sb = new StringBuilder("Compilation failed: ");
			diagnostics.getDiagnostics().forEach(d -> sb.append(d.getMessage(null)).append("\n"));
			throw new RuntimeException(sb.toString());
		}
		try {
			return memManager.getClassLoader(null).loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static final Pattern PUBLIC_CLASS = Pattern.compile("(?m)^\\s*public\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");
	private static final Pattern ANY_CLASS = Pattern.compile("(?m)^\\s*class\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");

	private static String detectClassName(String src) {
		Matcher m = PUBLIC_CLASS.matcher(src);
		if (m.find()) return m.group(1);
		m = ANY_CLASS.matcher(src);
		if (m.find()) return m.group(1);
		return null;
	}

	private static String stripPackage(String src) {
		// Remove any package declaration lines to keep classes package-less.
		return src.replaceAll("(?m)^\\s*package\\s+[^;]+;\\s*", "");
	}

	static class JavaSourceFromString extends SimpleJavaFileObject {
		final String code;
		JavaSourceFromString(String name, String code) {
			super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}
		@Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return code; }
	}

	static class InMemoryClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
		private final Map<String, InMemoryClassObject> classes = new ConcurrentHashMap<>();
		InMemoryClassFileManager(StandardJavaFileManager fileManager) { super(fileManager); }
		@Override public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
			InMemoryClassObject obj = new InMemoryClassObject(className, kind);
			classes.put(className, obj);
			return obj;
		}
		@Override public ClassLoader getClassLoader(Location location) {
			return new ClassLoader(getClass().getClassLoader()) {
				@Override protected Class<?> findClass(String name) throws ClassNotFoundException {
					InMemoryClassObject obj = classes.get(name);
					if (obj == null) throw new ClassNotFoundException(name);
					byte[] bytes = obj.getBytes();
					return defineClass(name, bytes, 0, bytes.length);
				}
			};
		}
	}

	static class InMemoryClassObject extends SimpleJavaFileObject {
		private final ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
		InMemoryClassObject(String name, Kind kind) {
			super(URI.create("mem:///" + name.replace('.', '/') + kind.extension), kind);
		}
		@Override public java.io.OutputStream openOutputStream() throws IOException { return baos; }
		byte[] getBytes() { return baos.getBytes(); }
	}

	static class ByteArrayOutputStreamEx extends java.io.ByteArrayOutputStream {
		byte[] getBytes() { return this.toByteArray(); }
	}
}


