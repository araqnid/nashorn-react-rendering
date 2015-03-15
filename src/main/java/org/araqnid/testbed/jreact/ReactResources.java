package org.araqnid.testbed.jreact;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import com.google.common.io.Resources;

public final class ReactResources {
	public static final String REACT_VERSION;

	static {
		URL propertiesUrl = Resources.getResource(ReactResources.class, "react.version.properties");
		Properties properties = new Properties();
		try {
			try (InputStream instream = Resources.asByteSource(propertiesUrl).openStream()) {
				properties.load(instream);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load " + propertiesUrl, e);
		}
		REACT_VERSION = properties.getProperty("react.version");
	}

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
