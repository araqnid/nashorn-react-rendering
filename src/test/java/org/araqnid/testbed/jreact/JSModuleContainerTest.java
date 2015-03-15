package org.araqnid.testbed.jreact;

import java.io.IOException;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.JSObject;

import org.junit.Test;

import static org.araqnid.testbed.jreact.JSObjectMatchers.jsFunction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

public class JSModuleContainerTest {
	@Test
	public void loads_module_with_no_dependencies() throws IOException, ScriptException {
		JSModuleContainer container = new JSModuleContainer("test");
		Object result = container.require("noDependencies");
		assertThat(result, equalTo("noDependencies module"));
	}

	@Test
	public void loads_module_with_empty_dependencies() throws IOException, ScriptException {
		JSModuleContainer container = new JSModuleContainer("test");
		Object result = container.require("emptyDependencies");
		assertThat(result, equalTo("emptyDependencies module"));
	}

	@Test
	public void loads_module_with_dependency() throws IOException, ScriptException {
		JSModuleContainer container = new JSModuleContainer("test");
		Object result = container.require("singleDependency");
		assertThat(result, equalTo("depends on <noDependencies module>"));
	}

	@Test
	public void loads_react() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		JSObject react = (JSObject) container.require("react");
		assertThat((JSObject) react.getMember("renderToStaticMarkup"), jsFunction());
	}

	@Test
	public void exposes_react_as_adaptor() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		container.require("react", JSModuleContainer.React.class);
	}

	@Test
	public void traps_attempt_to_expose_module_with_incompatible_adaptor() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		String moduleName = "react";
		Class<?> targetClass = Runnable.class;
		try {
			container.require(moduleName, targetClass);
			fail();
		} catch (ClassCastException e) {
			assertThat(e.getMessage(), both(containsString(moduleName)).and(containsString(targetClass.getName())));
		}
	}

	@Test
	public void traps_attempt_to_adapt_module_that_is_not_a_function() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		String moduleName = "noDependencies";
		Class<?> targetClass = Runnable.class;
		try {
			container.require(moduleName, targetClass);
			fail();
		} catch (ClassCastException e) {
			assertThat(e.getMessage(), both(containsString(moduleName)).and(containsString(targetClass.getName())));
		}
	}

	@Test
	public void loads_jsx_transformer() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		JSObject jsxTransformer = (JSObject) container.require("JSXTransformer");
		assertThat((JSObject) jsxTransformer.getMember("exec"), jsFunction());
	}

	@Test
	public void exposes_jsx_transformer_as_adaptor() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		container.require("JSXTransformer", JSModuleContainer.JSXTransformer.class);
	}

	@Test
	public void loads_jsx_module() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		JSObject jsxComponent = (JSObject) container.require("jsx!Component");
		JSModuleContainer.React react = container.require("react", JSModuleContainer.React.class);
		assertThat(react.renderToStaticMarkup(react.createElement(jsxComponent)),
				equalTo("<div>Component content</div>"));
	}

	@Test
	public void loads_dependent_jsx_module() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		JSObject jsxComponent = (JSObject) container.require("jsx!AggregateComponent");
		JSModuleContainer.React react = container.require("react", JSModuleContainer.React.class);
		assertThat(react.renderToStaticMarkup(react.createElement(jsxComponent)),
				equalTo("<ul><li><div>Component1 content</div></li><li><div>Component2 content</div></li></ul>"));
	}

	@Test
	public void console_log_available_to_js_module() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logMessage();
	}

	@Test
	public void console_log_accepts_multiple_parameters() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logMessageWithObject();
	}

	@Test
	public void console_info_available_to_js_module() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logInfo();
	}

	@Test
	public void console_info_accepts_multiple_parameters() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logInfoWithObject();
	}

	@Test
	public void console_warn_available_to_js_module() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logWarning();
	}

	@Test
	public void console_warn_accepts_multiple_parameters() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logWarningWithObject();
	}

	@Test
	public void console_error_available_to_js_module() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logError();
	}

	@Test
	public void console_error_accepts_multiple_parameters() throws Exception {
		new JSModuleContainer("test").require("consoleLogger", ConsoleLogger.class).logErrorWithObject();
	}

	public interface ConsoleLogger {
		void logMessage();

		void logMessageWithObject();

		void logInfo();

		void logInfoWithObject();

		void logWarning();

		void logWarningWithObject();

		void logError();

		void logErrorWithObject();
	}
}
