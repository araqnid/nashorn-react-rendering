package org.araqnid.testbed.jreact;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

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
		Queue<ScriptObjectMirror> invocations = new ConcurrentLinkedQueue<>();
		engineBindings.put("__loader", new Loader() {
			@Override
			public void define(ScriptObjectMirror args) {
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
		Queue<ScriptObjectMirror> invocations = new ConcurrentLinkedQueue<>();
		engineBindings.put("__loader", new Loader() {
			@Override
			public void define(ScriptObjectMirror args) {
				invocations.add(args);
			}
		});
		nashornEngine.eval("this.define = function() { __loader.define(arguments) }");
		nashornEngine.eval("define(function() { return \"content\"; })");
		ScriptObjectMirror invocation = invocations.poll();
		ScriptObjectMirror callback = (ScriptObjectMirror) invocation.getSlot(0);
		Object result = callback.call(null);
		assertThat(result, equalTo("content"));
	}

	@Test
	public void can_callback_with_an_array_and_a_function() throws Exception {
		Bindings engineBindings = nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE);
		Queue<ScriptObjectMirror> invocations = new ConcurrentLinkedQueue<>();
		engineBindings.put("__loader", new Loader() {
			@Override
			public void define(ScriptObjectMirror args) {
				invocations.add(args);
			}
		});
		nashornEngine.eval("this.define = function() { __loader.define(arguments) }");
		nashornEngine.eval("define([\"bar\"], function(bar) { return \"content\"; })");
		assertThat(invocations, contains(jsArrayishContaining(jsArray(), jsFunction())));
	}

	private static Matcher<ScriptObjectMirror> jsArrayishContaining(Matcher<ScriptObjectMirror> elementMatcher) {
		return new TypeSafeDiagnosingMatcher<ScriptObjectMirror>() {
			@Override
			protected boolean matchesSafely(ScriptObjectMirror item, Description mismatchDescription) {
				if (item.isEmpty()) {
					mismatchDescription.appendText("empty");
					return false;
				}
				if (item.size() > 1) {
					mismatchDescription.appendText("more than 1: ").appendValue(item);
					return false;
				}
				Object obj = item.getSlot(0);
				elementMatcher.describeMismatch(obj, mismatchDescription);
				return elementMatcher.matches(obj);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("[").appendDescriptionOf(elementMatcher).appendText("]");
			}
		};
	}

	private static Matcher<ScriptObjectMirror> jsArrayishContaining(Matcher<ScriptObjectMirror> firstElementMatcher,
			Matcher<ScriptObjectMirror> secondElementMatcher) {
		return new TypeSafeDiagnosingMatcher<ScriptObjectMirror>() {
			@Override
			protected boolean matchesSafely(ScriptObjectMirror item, Description mismatchDescription) {
				if (item.isEmpty()) {
					mismatchDescription.appendText("empty");
					return false;
				}
				if (item.size() != 2) {
					mismatchDescription.appendText("not 2 elements: ").appendValue(item);
					return false;
				}
				Object firstElement = item.getSlot(0);
				if (!firstElementMatcher.matches(firstElement)) {
					mismatchDescription.appendText("[0]: ");
					firstElementMatcher.describeMismatch(firstElement, mismatchDescription);
					return false;
				}
				Object secondElement = item.getSlot(1);
				if (!secondElementMatcher.matches(secondElement)) {
					mismatchDescription.appendText("[1]: ");
					secondElementMatcher.describeMismatch(secondElement, mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("[").appendDescriptionOf(firstElementMatcher).appendText(", ")
						.appendDescriptionOf(secondElementMatcher).appendText("]");
			}
		};
	}

	private static Matcher<ScriptObjectMirror> jsArray() {
		return new TypeSafeDiagnosingMatcher<ScriptObjectMirror>() {
			@Override
			protected boolean matchesSafely(ScriptObjectMirror item, Description mismatchDescription) {
				mismatchDescription.appendValue(item);
				return item.isArray();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("an array");
			}
		};
	}

	private static Matcher<ScriptObjectMirror> jsFunction() {
		return new TypeSafeDiagnosingMatcher<ScriptObjectMirror>() {
			@Override
			protected boolean matchesSafely(ScriptObjectMirror item, Description mismatchDescription) {
				mismatchDescription.appendValue(item);
				return item.isFunction();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("a function");
			}
		};
	}

	public interface Loader {
		void define(ScriptObjectMirror function);
	}
}