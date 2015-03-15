package org.araqnid.testbed.jreact;

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
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import static com.google.common.base.Verify.verifyNotNull;

public class JSModuleContainer {
	private static final Pattern JSX_PATTERN = Pattern.compile("jsx!(.+)");
	private final ScriptEngineManager engineManager = new ScriptEngineManager();
	private final ScriptEngine nashornEngine = engineManager.getEngineByName("nashorn");
	private final Map<String, Module> modules = new HashMap<>();
	private final String root;

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
		if (!(module.value instanceof JSObject)) { throw new ClassCastException("Non-object module '" + moduleName
				+ "' is not compatible with " + targetInterface); }
		T adaptor = nashornInvoker.getInterface(module.value, targetInterface);
		if (adaptor == null)
			throw new ClassCastException("Module '" + moduleName + "' is not compatible with " + targetInterface);
		module.adaptors = ImmutableClassToInstanceMap.builder().putAll(module.adaptors).put(targetInterface, adaptor)
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
		Module module = new Module();
		modules.put(moduleName, module);
		URL resource = Resources.getResource(root + "/" + moduleName + ".js");
		JSObject defineCall = loadModuleFactory(moduleName, resource,
				Resources.asCharSource(resource, StandardCharsets.UTF_8));
		define(module, moduleName, defineCall);
		return module;
	}

	private JSObject loadModuleFactory(String moduleName, URL resource, CharSource charSource) throws ScriptException,
			IOException {
		ScriptContext scriptContext = new SimpleScriptContext();
		Bindings engineBindings = nashornEngine.createBindings();
		scriptContext.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
		LoaderProxy loaderProxy = new LoaderProxy();
		engineBindings.put("__loader", loaderProxy);
		engineBindings.put("console", new Console(moduleName.replace('/', '_')));
		engineBindings.put("define", nashornEngine.eval("(function() { __loader.define(arguments) })", scriptContext));
		scriptContext.setAttribute(ScriptEngine.FILENAME, resource.toString(), ScriptContext.ENGINE_SCOPE);
		try (BufferedReader reader = charSource.openBufferedStream()) {
			nashornEngine.eval(reader, scriptContext);
		}
		if (loaderProxy.defineCalls.isEmpty()) throw new IllegalStateException("No call to define() from " + resource);
		JSObject defineCall = loaderProxy.defineCalls.poll();
		if (!loaderProxy.defineCalls.isEmpty())
			throw new IllegalStateException("Multiple calls to define() from " + resource);
		return defineCall;
	}

	private void define(Module module, String moduleName, JSObject defineCall) throws IOException, ScriptException {
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
		Module module = new Module();
		modules.put(moduleName, module);
		URL resource = Resources.getResource(root + "/" + residualName + ".jsx");
		JSXTransformer adaptor = modules.get("JSXTransformer").adaptors.getInstance(JSXTransformer.class);
		JSObject jsTransformOutput = adaptor.transform(Resources.asCharSource(resource, StandardCharsets.UTF_8).read());
		String jsSource = (String) jsTransformOutput.getMember("code");
		JSObject defineCall = loadModuleFactory(residualName, resource, CharSource.wrap(jsSource));
		define(module, moduleName, defineCall);
		return module;
	}

	private void ensureReactLoaded() {
		Module reactModule = modules.get("react");
		if (reactModule != null) {
			if (reactModule.state == Module.State.LOADED) return;
			throw new IllegalStateException("React module is in state " + reactModule.state);
		}
		try {
			loadReactModule("JSXTransformer", "jsx-transformer.js", "JSXTransformer", JSXTransformer.class);
		} catch (IOException | ScriptException e) {
			throw new IllegalStateException("Unable to load JSXTransformer", e);
		}
		try {
			loadReactModule("react", "react-with-addons.js", "React", React.class);
		} catch (IOException | ScriptException e) {
			throw new IllegalStateException("Unable to load React", e);
		}
	}

	private <T> Module loadReactModule(String moduleName, String scriptName, String symbol, Class<T> adaptTo)
			throws IOException, ScriptException {
		Module module = new Module();
		modules.put(moduleName, module);
		module.value = loadReactScript(Resources.getResource("web/" + scriptName), symbol);
		Invocable nashornInvoker = (Invocable) nashornEngine;
		module.adaptors = ImmutableClassToInstanceMap.builder()
				.put(adaptTo, nashornInvoker.getInterface(module.value, adaptTo)).build();
		module.state = Module.State.LOADED;
		return module;
	}

	private JSObject loadReactScript(URL resource, String symbol) throws IOException, ScriptException {
		ScriptContext scriptContext = new SimpleScriptContext();
		Bindings engineBindings = nashornEngine.createBindings();
		scriptContext.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
		engineBindings.put("__loader", new LoaderProxy());
		engineBindings.put("console", new Console(symbol));
		nashornEngine.eval("var global = this", scriptContext);
		scriptContext.setAttribute(ScriptEngine.FILENAME, resource.toString(), ScriptContext.ENGINE_SCOPE);
		CharSource charSource = Resources.asCharSource(resource, StandardCharsets.UTF_8);
		try (BufferedReader reader = charSource.openBufferedStream()) {
			nashornEngine.eval(reader, scriptContext);
		}
		return (JSObject) verifyNotNull(scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).get(symbol));
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

	public static class LoaderProxy {
		final Queue<JSObject> defineCalls = new ConcurrentLinkedQueue<>();

		public void define(JSObject args) {
			defineCalls.add(args);
		}

		@Override
		public String toString() {
			return "LoaderProxy";
		}
	}

	public static class Console {
		private final Logger logger;

		private Console(String context) {
			logger = LoggerFactory.getLogger(JSModuleContainer.class.getName() + ".JS." + context);
		}

		public void log(String message) {
			logger.info(message);
		}

		public void warn(String message) {
			logger.warn(message);
		}

		public void error(String message) {
			logger.error(message);
		}
	}

	public interface JSXTransformer {
		void exec(String str);

		JSObject transform(String source);
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
