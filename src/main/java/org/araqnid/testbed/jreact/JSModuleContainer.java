package org.araqnid.testbed.jreact;

import static com.google.common.base.Verify.verifyNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.JSObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Verify;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

public class JSModuleContainer {
	private static final Pattern JSX_PATTERN = Pattern.compile("jsx!(.+)");
	private final ScriptEngineManager engineManager = new ScriptEngineManager();
	private final ScriptEngine nashornEngine = engineManager.getEngineByName("nashorn");
	private final Map<String, Module> modules = new HashMap<>();
	private final String root;
	private final Queue<JSObject> defineCalls = new ConcurrentLinkedQueue<>();

	public JSModuleContainer(String root) {
		this.root = root;
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

	public <T> T require(String moduleName, Class<T> targetInterface) throws IOException, ScriptException {
		Module module = modules.get(moduleName);
		if (module != null) {
			if (module.state != Module.State.LOADED)
				throw new IllegalStateException(moduleName + " in state " + module.state);
		}
		else {
			module = load(moduleName);
		}
		if (module.adaptors.containsKey(targetInterface)) return module.adaptors.getInstance(targetInterface);
		Invocable nashornInvoker = (Invocable) nashornEngine;
		if (!(module.value instanceof JSObject)) {
			throw new ClassCastException("Non-object module '" + moduleName + "' is not compatible with " + targetInterface);
		}
		T adaptor = nashornInvoker.getInterface(module.value, targetInterface);
		if (adaptor == null) throw new ClassCastException("Module '" + moduleName + "' is not compatible with " + targetInterface);
		module.adaptors = ImmutableClassToInstanceMap.builder()
				.putAll(module.adaptors)
				.put(targetInterface, adaptor)
				.build();
		return adaptor;
	}

	private Module load(String moduleName) throws IOException, ScriptException {
		Matcher jsxMatcher = JSX_PATTERN.matcher(moduleName);
		if (jsxMatcher.matches()) {
			return loadJSX(moduleName, jsxMatcher.group(1));
		}
		else if (moduleName.equals("react") || moduleName.equals("JSXTransformer")) {
			ensureReactLoaded();
			return modules.get(moduleName);
		}
		else {
			return loadBasic(moduleName);
		}
	}

	private Module loadBasic(String moduleName) throws IOException, ScriptException {
		Verify.verify(defineCalls.isEmpty());
		Module module = new Module();
		modules.put(moduleName, module);
		ScriptContext scriptContext = new SimpleScriptContext();
		Bindings engineBindings = nashornEngine.createBindings();
		scriptContext.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
		String resourceName = root + "/" + moduleName + ".js";
		URL resource = Resources.getResource(resourceName);
		CharSource charSource = Resources.asCharSource(resource, StandardCharsets.UTF_8);
		engineBindings.put("__loader", new LoaderProxy());
		engineBindings.put("console", new Console());
		engineBindings.put("define", nashornEngine.eval("(function() { __loader.define(arguments) })", scriptContext));
		scriptContext.setAttribute(ScriptEngine.FILENAME, resource.toString(), ScriptContext.ENGINE_SCOPE);
		try (BufferedReader reader = charSource.openBufferedStream()) {
			nashornEngine.eval(reader, scriptContext);
		}
		if (defineCalls.isEmpty()) throw new IllegalStateException("No call to define() from " + resourceName);
		JSObject defineCall = defineCalls.poll();
		if (!defineCalls.isEmpty()) throw new IllegalStateException("Multiple calls to define() from " + resourceName);
		define(module, moduleName, defineCall);
		return module;
	}

	private void define(Module module, String moduleName, JSObject defineCall) throws IOException,
			ScriptException {
		JSObject callback;
		List<String> dependencies;
		if (defineCall.values().size() == 1) {
			JSObject arg0 = (JSObject) defineCall.getSlot(0);
			if (!arg0.isFunction())
				throw new IllegalStateException(moduleName + ": single-arg call to define was not passed a function");
			callback = arg0;
			dependencies = ImmutableList.of();
		}
		else if (defineCall.values().size() == 2) {
			JSObject arg0 = (JSObject) defineCall.getSlot(0);
			dependencies = ImmutableList.copyOf(Collections2.transform(arg0.values(), Functions.toStringFunction()));
			JSObject arg1 = (JSObject) defineCall.getSlot(1);
			if (!arg1.isFunction())
				throw new IllegalStateException(moduleName + ": second argument to define was not passed a function");
			callback = arg1;
		}
		else {
			throw new IllegalStateException(moduleName + ": was passed " + defineCall.values().size() + " arguments");
		}
		List<Object> dependencyValues = Lists.newArrayListWithExpectedSize(dependencies.size());
		for (String dependencyName : dependencies) {
			dependencyValues.add(require(dependencyName));
		}
		module.value = callback.call(null, dependencyValues.toArray());
		module.state = Module.State.LOADED;
	}

	private Module loadJSX(String moduleName, String residualName) throws IOException, ScriptException {
		ensureReactLoaded();
		Verify.verify(defineCalls.isEmpty());
		Module module = new Module();
		modules.put(moduleName, module);
		String resourceName = root + "/" + residualName + ".jsx";
		String jsxSource = Resources.asCharSource(Resources.getResource(resourceName), StandardCharsets.UTF_8).read();
		JSXTransformer adaptor = modules.get("JSXTransformer").adaptors.getInstance(JSXTransformer.class);
		adaptor.exec( "(function(define) { " + jsxSource + " })(function() { __loader.define(arguments) })");
		if (defineCalls.isEmpty()) throw new IllegalStateException("No call to define() from " + resourceName);
		JSObject defineCall = defineCalls.poll();
		if (!defineCalls.isEmpty()) throw new IllegalStateException("Multiple calls to define() from " + resourceName);
		define(module, moduleName, defineCall);
		return module;
	}

	private void ensureReactLoaded() {
		Module reactModule = modules.get("react");
		if (reactModule != null) {
			if (reactModule.state == Module.State.LOADED) return;
			throw new IllegalStateException("React module is in state " + reactModule.state);
		}
		reactModule = new Module();
		Module jsxModule = new Module();
		modules.put("react", reactModule);
		modules.put("JSXTransformer", jsxModule);
		Invocable nashornInvoker = (Invocable) nashornEngine;
		try {
			ScriptContext scriptContext = new SimpleScriptContext();
			Bindings engineBindings = nashornEngine.createBindings();
			scriptContext.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
			engineBindings.put("__loader", new LoaderProxy());
			engineBindings.put("console", new Console());
			nashornEngine.eval("var global = this", scriptContext);
			loadScript("react-with-addons.js", scriptContext);
			reactModule.value = verifyNotNull(scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).get("React"));
			reactModule.adaptors = ImmutableClassToInstanceMap.builder()
					.put(React.class, nashornInvoker.getInterface(reactModule.value, React.class))
					.build();
			loadScript("jsx-transformer.js", scriptContext);
			jsxModule.value = verifyNotNull(scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).get("JSXTransformer"));
			jsxModule.adaptors = ImmutableClassToInstanceMap.builder()
					.put(JSXTransformer.class, nashornInvoker.getInterface(jsxModule.value, JSXTransformer.class))
					.build();
		} catch (ScriptException | IOException | ClassCastException e) {
			throw new IllegalStateException("Unable to load React/JSX", e);
		}
		jsxModule.state = Module.State.LOADED;
		reactModule.state = Module.State.LOADED;
	}

	private void loadScript(String src, ScriptContext scriptContext) throws IOException, ScriptException {
		URL resource = Resources.getResource("web/" + src);
		scriptContext.setAttribute(ScriptEngine.FILENAME, resource.toString(), ScriptContext.ENGINE_SCOPE);
		CharSource charSource = Resources.asCharSource(resource, StandardCharsets.UTF_8);
		try (BufferedReader reader = charSource.openBufferedStream()) {
			nashornEngine.eval(reader, scriptContext);
		}
	}

	private static final class Module {
		private static final ClassToInstanceMap<Object> NONE = ImmutableClassToInstanceMap.builder().build();

		enum State {
			LOADING, LOADED
		};

		public State state = State.LOADING;
		public Object value;
		public ClassToInstanceMap<Object> adaptors = NONE;
	}

	public class LoaderProxy {
		public void define(JSObject args) {
			defineCalls.add(args);
		}

		@Override
		public String toString() {
			return "LoaderProxy:" + JSModuleContainer.this.toString();
		}
	}

	public static class Console {
		private static final Logger LOG = LoggerFactory.getLogger(JSModuleContainer.class.getName() + ".JS");

		public void log(String message) {
			LOG.info(message);
		}

		public void warn(String message) {
			LOG.warn(message);
		}

		public void error(String message) {
			LOG.error(message);
		}
	}

	public interface JSXTransformer {
		void exec(String str);
	}

	public interface React {
		JSObject createElement(String str);
		JSObject createElement(String str, JSObject props, JSObject... children);
		JSObject createElement(JSObject factory);
		JSObject createElement(JSObject factory, JSObject props);
		String renderToStaticMarkup(JSObject element);
		String renderToString(JSObject element);
	}
}
