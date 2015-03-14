package org.araqnid.testbed.jreact;

import static org.araqnid.testbed.jreact.CallbackFromJavascriptTest.jsFunction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
	public void loads_jsx_transformer() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		ScriptObjectMirror jsxTransformer = (ScriptObjectMirror) container.require("JSXTransformer");
		assertThat((ScriptObjectMirror) jsxTransformer.get("exec"), jsFunction());
	}

	@Test
	public void loads_jsx_module() throws Exception {
		JSModuleContainer container = new JSModuleContainer("test");
		ScriptObjectMirror jsxComponent = (ScriptObjectMirror) container.require("jsx!Component");
		ScriptObjectMirror react = (ScriptObjectMirror) container.require("react");
		assertThat(react.callMember("renderToStaticMarkup", react.callMember("createElement", jsxComponent)),
				equalTo("<div>Component content</div>"));
	}
}
