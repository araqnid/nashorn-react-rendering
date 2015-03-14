package org.araqnid.testbed.jreact;

import static org.araqnid.testbed.jreact.CallbackFromJavascriptTest.jsFunction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.junit.Test;

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
		ScriptObjectMirror react = (ScriptObjectMirror) container.require("react");
		assertThat(react.callMember("renderToStaticMarkup", react.callMember("createElement", "div", null, "test")),
				equalTo("<div>test</div>"));
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
			assertThat(e.getMessage(),
					both(containsString(moduleName)).and(containsString(targetClass.getName())));
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
			assertThat(e.getMessage(),
					both(containsString(moduleName)).and(containsString(targetClass.getName())));
		}
	}

	@Test
	public void loads_jsx_transformer() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		ScriptObjectMirror jsxTransformer = (ScriptObjectMirror) container.require("JSXTransformer");
		assertThat((ScriptObjectMirror) jsxTransformer.get("exec"), jsFunction());
	}

	@Test
	public void exposes_jsx_transformer_as_adaptor() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		container.require("JSXTransformer", JSModuleContainer.JSXTransformer.class);
	}

	@Test
	public void loads_jsx_module() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		ScriptObjectMirror jsxComponent = (ScriptObjectMirror) container.require("jsx!Component");
		ScriptObjectMirror react = (ScriptObjectMirror) container.require("react");
		assertThat(react.callMember("renderToStaticMarkup", react.callMember("createElement", jsxComponent)),
				equalTo("<div>Component content</div>"));
	}

	@Test
	public void loads_dependent_jsx_module() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		ScriptObjectMirror jsxComponent = (ScriptObjectMirror) container.require("jsx!AggregateComponent");
		ScriptObjectMirror react = (ScriptObjectMirror) container.require("react");
		assertThat(react.callMember("renderToStaticMarkup", react.callMember("createElement", jsxComponent)),
				equalTo("<ul><li><div>Component1 content</div></li><li><div>Component2 content</div></li></ul>"));
	}
}
