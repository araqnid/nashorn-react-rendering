package org.araqnid.testbed.jreact;

import java.net.URL;

import com.google.common.io.Resources;

public final class ReactResources {
	public static final String REACT_VERSION = "0.12.1";

	public static URL resourceFor(String script) {
		return Resources.getResource(String.format("META-INF/resources/webjars/react/%s/%s.js", REACT_VERSION, script));
	}

	public static URL reactScript() {
		return resourceFor("react-with-addons");
	}

	public static URL jsxTransformerScript() {
		return resourceFor("JSXTransformer");
	}

	private ReactResources() {
	}
}
