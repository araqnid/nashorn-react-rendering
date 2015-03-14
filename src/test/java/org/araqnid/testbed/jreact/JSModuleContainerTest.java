package org.araqnid.testbed.jreact;

import java.io.IOException;

import javax.script.ScriptException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
}
