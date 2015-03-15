package org.araqnid.testbed.jreact;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.JSObject;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import static org.araqnid.testbed.jreact.JSObjectMatchers.jsEmptyObject;
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
	public void renders_component_with_data_from_props() throws Exception {
		nashornEngine.eval("global = {};");

		String str = "xyzzyString";
		Object rawProps = ImmutableMap.of("content", str);

		Json json = nashornInvoker.getInterface(nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE).get("JSON"),
				Json.class);
		JSObject props = json.parse(new ObjectMapper().writeValueAsString(rawProps));

		loadScript("react-with-addons.js");
		Object jsReact = nashornEngine.eval("global.React");

		Object componentBody = nashornEngine
				.eval("(function(){ return { render: function() { return global.React.createElement(\"div\", {}, \"content: \" + this.props.content) } } })()");
		Object component = nashornInvoker.invokeMethod(jsReact, "createClass", componentBody);
		Object renderElement = nashornInvoker.invokeMethod(jsReact, "createElement", component, props);
		Object renderOutput = nashornInvoker.invokeMethod(jsReact, "renderToStaticMarkup", renderElement);
		assertThat(renderOutput, equalTo("<div>content: " + str + "</div>"));
	}

	@Test
	public void transforms_jsx_source() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("jsx-transformer.js");
		JSXTransformer jsxTransformer = nashornInvoker.getInterface(nashornEngine.eval("global.JSXTransformer"),
				JSXTransformer.class);

		String source = "var content = <Content>foo</Content>;";

		JSObject transformOutput = jsxTransformer.transform(source);
		assertThat(transformOutput.keySet(), equalTo(ImmutableSet.of("code", "extra")));
		assertThat(transformOutput.getMember("code"),
				equalTo("var content = React.createElement(Content, null, \"foo\");"));
		assertThat((JSObject) transformOutput.getMember("extra"), jsEmptyObject());
	}

	@Test
	public void transforms_jsx_source_with_options() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("jsx-transformer.js");
		JSXTransformer jsxTransformer = nashornInvoker.getInterface(nashornEngine.eval("global.JSXTransformer"),
				JSXTransformer.class);

		String source = "var content = <Content>foo</Content>;";
		JSXTransformer.Options options = new JSXTransformer.Options(false, false);

		JSObject transformOutput = jsxTransformer.transform(source, options);
		assertThat(transformOutput.keySet(), equalTo(ImmutableSet.of("code", "extra")));
		assertThat(transformOutput.getMember("code"),
				equalTo("var content = React.createElement(Content, null, \"foo\");"));
		assertThat((JSObject) transformOutput.getMember("extra"), jsEmptyObject());
	}

	@Test
	public void transforms_jsx_source_with_options_specifying_harmony_transform() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("jsx-transformer.js");
		JSXTransformer jsxTransformer = nashornInvoker.getInterface(nashornEngine.eval("global.JSXTransformer"),
				JSXTransformer.class);

		String source = "var f = v => this.props[v];";
		JSXTransformer.Options options = new JSXTransformer.Options(true, false);

		JSObject transformOutput = jsxTransformer.transform(source, options);
		assertThat(transformOutput.keySet(), equalTo(ImmutableSet.of("code", "extra")));
		assertThat(transformOutput.getMember("code"),
				equalTo("var f = function(v)  {return this.props[v];}.bind(this);"));
		assertThat((JSObject) transformOutput.getMember("extra"), jsEmptyObject());
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

	@Test
	public void react_symbol_can_be_added_to_implicit_global() throws Exception {
		nashornEngine.eval("global = {};");

		loadScript("react-with-addons.js");
		Object jsReact = nashornEngine.eval("global.React");
		Bindings bindings = nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put("React", jsReact);

		loadScript("jsx-transformer.js");
		Object jsJSXTransformer = nashornEngine.eval("global.JSXTransformer");

		Object jsxComponent = nashornInvoker.invokeMethod(jsJSXTransformer, "exec",
				"React.createClass({ render: function() { return <div>JSX component</div>; } })");
		Object renderElement = nashornInvoker.invokeMethod(jsReact, "createElement", jsxComponent);
		Object renderOutput = nashornInvoker.invokeMethod(jsReact, "renderToStaticMarkup", renderElement);
		assertThat(renderOutput, equalTo("<div>JSX component</div>"));
	}

	@Test
	public void modules_can_export_directly_to_implicit_global() throws Exception {
		nashornEngine.eval("var global = this");

		loadScript("react-with-addons.js");
		assertThat(nashornEngine.eval("React.version"), equalTo("0.12.1"));

		assertThat(nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE).get("React"), not(nullValue()));
	}

	private static String withReactSymbol(String js) {
		return String.format("(function(React) { return %s })(global.React)", js);
	}

	private void loadScript(String src) throws IOException, ScriptException {
		CharSource charSource = Resources.asCharSource(Resources.getResource("web/" + src), StandardCharsets.UTF_8);
		nashornEngine.getContext().setAttribute(ScriptEngine.FILENAME, src, ScriptContext.ENGINE_SCOPE);
		try (BufferedReader reader = charSource.openBufferedStream()) {
			nashornEngine.eval(reader);
		} finally {
			nashornEngine.getContext().removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
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

	public interface Json {
		JSObject parse(String str);
	}

	public interface JSXTransformer {
		JSObject transform(String source);

		JSObject transform(String source, Options options);

		public class Options {
			private final boolean harmony;
			private final boolean stripTypes;

			public Options(boolean harmony, boolean stripTypes) {
				this.harmony = harmony;
				this.stripTypes = stripTypes;
			}

			public boolean isHarmony() {
				return harmony;
			}

			public boolean isStripTypes() {
				return stripTypes;
			}
		}
	}
}
