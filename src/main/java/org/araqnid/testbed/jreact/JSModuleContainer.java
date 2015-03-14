package org.araqnid.testbed.jreact;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

public class JSModuleContainer {
	private final ScriptEngineManager engineManager = new ScriptEngineManager();
	private final ScriptEngine nashornEngine = engineManager.getEngineByName("nashorn");
	private final Map<String, Module> modules = new HashMap<>();
	private final String root;
	private final Queue<ScriptObjectMirror> defineCalls = new ConcurrentLinkedQueue<>();

	public JSModuleContainer(String root) {
		this.root = root;
		try {
			Bindings engineBindings = nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE);
			engineBindings.put("__loader", new LoaderProxy());
			nashornEngine.eval("this.define = function() { __loader.define(arguments) }");
		} catch (ScriptException e) {
			throw new IllegalStateException("Unable to set up Nashorn engine", e);
		}
	}

	public Object require(String moduleName) throws IOException, ScriptException {
		Module module = modules.get(moduleName);
		if (module != null) {
			if (module.state != Module.State.LOADED)
				throw new IllegalStateException(moduleName + " in state " + module.state);
		}
		else {
			module = load(moduleName);
		}
		return module.value;
	}

	private Module load(String moduleName) throws IOException, ScriptException {
		Verify.verify(defineCalls.isEmpty());
		Module module = new Module();
		modules.put(moduleName, module);
		String resourceName = root + "/" + moduleName + ".js";
		CharSource charSource = Resources.asCharSource(Resources.getResource(resourceName), StandardCharsets.UTF_8);
		try (BufferedReader reader = charSource.openBufferedStream()) {
			nashornEngine.eval(reader);
		}
		if (defineCalls.isEmpty()) throw new IllegalStateException("No call to define() from " + resourceName);
		ScriptObjectMirror defineCall = defineCalls.poll();
		if (!defineCalls.isEmpty()) throw new IllegalStateException("Multiple calls to define() from " + resourceName);
		define(module, moduleName, defineCall);
		return module;
	}

	private void define(Module module, String moduleName, ScriptObjectMirror defineCall) throws IOException,
			ScriptException {
		ScriptObjectMirror callback;
		List<String> dependencies;
		if (defineCall.size() == 1) {
			ScriptObjectMirror arg0 = (ScriptObjectMirror) defineCall.getSlot(0);
			if (!arg0.isFunction())
				throw new IllegalStateException(moduleName + ": single-arg call to define was not passed a function");
			callback = arg0;
			dependencies = ImmutableList.of();
		}
		else if (defineCall.size() == 2) {
			ScriptObjectMirror arg0 = (ScriptObjectMirror) defineCall.getSlot(0);
			List<String> foundDependencies = Lists.newArrayListWithExpectedSize(arg0.size());
			for (int i = 0; i < arg0.size(); i++) {
				Object value = arg0.getSlot(i);
				foundDependencies.add(value.toString());
			}
			ScriptObjectMirror arg1 = (ScriptObjectMirror) defineCall.getSlot(1);
			if (!arg1.isFunction())
				throw new IllegalStateException(moduleName + ": second argument to define was not passed a function");
			callback = arg1;
			dependencies = ImmutableList.copyOf(foundDependencies);
		}
		else {
			throw new IllegalStateException(moduleName + ": was passed " + defineCall.size() + " arguments");
		}
		List<Object> dependencyValues = Lists.newArrayListWithExpectedSize(dependencies.size());
		for (String dependencyName : dependencies) {
			dependencyValues.add(require(dependencyName));
		}
		module.value = callback.call(null, dependencyValues.toArray());
		module.state = Module.State.LOADED;
	}

	private static final class Module {
		enum State {
			LOADING, LOADED
		};

		public State state = State.LOADING;
		public Object value;
	}

	public class LoaderProxy {
		public void define(ScriptObjectMirror args) {
			defineCalls.add(args);
		}

		@Override
		public String toString() {
			return "LoaderProxy:" + JSModuleContainer.this.toString();
		}
	}
}
