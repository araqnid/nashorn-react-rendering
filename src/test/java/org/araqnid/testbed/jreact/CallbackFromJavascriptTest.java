package org.araqnid.testbed.jreact;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.JSObject;

import org.junit.Test;

import static org.araqnid.testbed.jreact.JSObjectMatchers.jsArray;
import static org.araqnid.testbed.jreact.JSObjectMatchers.jsArrayishContaining;
import static org.araqnid.testbed.jreact.JSObjectMatchers.jsFunction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class CallbackFromJavascriptTest {
	private final ScriptEngineManager engineManager = new ScriptEngineManager();
	private final ScriptEngine nashornEngine = engineManager.getEngineByName("nashorn");
	@SuppressWarnings("unused")
	private final Invocable nashornInvoker = (Invocable) nashornEngine;

	@Test
	public void can_callback_with_a_function() throws Exception {
		Bindings engineBindings = nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE);
		Queue<JSObject> invocations = new ConcurrentLinkedQueue<>();
		engineBindings.put("__loader", new Loader() {
			@Override
			public void define(JSObject args) {
				invocations.add(args);
			}
		});
		nashornEngine.eval("this.define = function() { __loader.define(arguments) }");
		nashornEngine.eval("define(function() { return \"content\"; })");
		assertThat(invocations, contains(jsArrayishContaining(jsFunction())));
	}

	@Test
	public void can_call_the_callback() throws Exception {
		Bindings engineBindings = nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE);
		Queue<JSObject> invocations = new ConcurrentLinkedQueue<>();
		engineBindings.put("__loader", new Loader() {
			@Override
			public void define(JSObject args) {
				invocations.add(args);
			}
		});
		nashornEngine.eval("this.define = function() { __loader.define(arguments) }");
		nashornEngine.eval("define(function() { return \"content\"; })");
		JSObject invocation = invocations.poll();
		JSObject callback = (JSObject) invocation.getSlot(0);
		Object result = callback.call(null);
		assertThat(result, equalTo("content"));
	}

	@Test
	public void can_callback_with_an_array_and_a_function() throws Exception {
		Bindings engineBindings = nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE);
		Queue<JSObject> invocations = new ConcurrentLinkedQueue<>();
		engineBindings.put("__loader", new Loader() {
			@Override
			public void define(JSObject args) {
				invocations.add(args);
			}
		});
		nashornEngine.eval("this.define = function() { __loader.define(arguments) }");
		nashornEngine.eval("define([\"bar\"], function(bar) { return \"content\"; })");
		assertThat(invocations, contains(jsArrayishContaining(jsArray(), jsFunction())));
	}

	public interface Loader {
		void define(JSObject function);
	}
}
