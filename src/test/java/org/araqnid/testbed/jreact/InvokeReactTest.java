package org.araqnid.testbed.jreact;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class InvokeReactTest {
	private final ScriptEngineManager engineManager = new ScriptEngineManager();
	private final ScriptEngine nashornEngine = engineManager.getEngineByName("nashorn");
	private final Invocable nashornInvoker = (Invocable) nashornEngine;

	@Test
	public void react_can_be_loaded() throws Exception {
		nashornEngine.eval("global = {};");
		loadScript("react-with-addons.js");
		assertThat(nashornEngine.eval("global.React.version"), equalTo("0.12.1"));
	}

	@Test
	public void jsx_transformer_can_be_loaded() throws Exception {
		nashornEngine.eval("global = {};");
		loadScript("jsx-transformer.js");
		assertThat(nashornEngine.eval("global.JSXTransformer"), not(nullValue()));
	}

	@Test
	public void renders_static_element() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("react-with-addons.js");
		Object jsReact = nashornEngine.eval("global.React");

		Object element = nashornInvoker.invokeMethod(jsReact, "createElement", "div", null, "Static content");
		Object renderOutput = nashornInvoker.invokeMethod(jsReact, "renderToStaticMarkup", element);

		assertThat(renderOutput, equalTo("<div>Static content</div>"));
	}

	@Test
	public void renders_static_element_with_react_ids() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("react-with-addons.js");
		Object jsReact = nashornEngine.eval("global.React");

		Object element = nashornInvoker.invokeMethod(jsReact, "createElement", "div", null, "Static content");
		Object renderOutput = nashornInvoker.invokeMethod(jsReact, "renderToString", element);

		assertThat((String) renderOutput,
				matches("<div data-reactid=\"[^\"]+\" data-react-checksum=\"-?[0-9]+\">Static content</div>"));
	}

	@Test
	public void renders_static_component() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("react-with-addons.js");
		Object jsReact = nashornEngine.eval("global.React");

		Object componentBody = nashornEngine
				.eval("(function(){ return { render: function() { return global.React.createElement(\"div\", {}, \"Component content\") } } })()");
		Object component = nashornInvoker.invokeMethod(jsReact, "createClass", componentBody);
		Object renderElement = nashornInvoker.invokeMethod(jsReact, "createElement", component);
		Object renderOutput = nashornInvoker.invokeMethod(jsReact, "renderToStaticMarkup", renderElement);
		assertThat(renderOutput, equalTo("<div>Component content</div>"));
	}

	@Test
	public void renders_jsx_component() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("react-with-addons.js");
		Object jsReact = nashornEngine.eval("global.React");

		loadScript("jsx-transformer.js");
		Object jsJSXTransformer = nashornEngine.eval("global.JSXTransformer");

		Object jsxComponent = nashornInvoker.invokeMethod(jsJSXTransformer, "exec",
				withReactSymbol("React.createClass({ render: function() { return <div>JSX component</div>; } })"));
		Object renderElement = nashornInvoker.invokeMethod(jsReact, "createElement", jsxComponent);
		Object renderOutput = nashornInvoker.invokeMethod(jsReact, "renderToStaticMarkup", renderElement);
		assertThat(renderOutput, equalTo("<div>JSX component</div>"));
	}

	private static String withReactSymbol(String js) {
		return String.format("(function(React) { return %s })(global.React)", js);
	}

	private void loadScript(String src) throws IOException, ScriptException {
		CharSource charSource = Resources.asCharSource(Resources.getResource("web/" + src), StandardCharsets.UTF_8);
		try (BufferedReader reader = charSource.openBufferedStream()) {
			nashornEngine.eval(reader);
		}
	}

	private static Matcher<String> matches(String regex) {
		return new TypeSafeDiagnosingMatcher<String>() {
			private final Pattern pattern = Pattern.compile(regex);

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				mismatchDescription.appendText("was ");
				mismatchDescription.appendValue(item);
				return pattern.matcher(item).matches();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("matching ").appendValue(pattern.pattern());
			}
		};
	}
}
