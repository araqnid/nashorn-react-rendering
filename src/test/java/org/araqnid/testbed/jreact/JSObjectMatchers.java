package org.araqnid.testbed.jreact;

import jdk.nashorn.api.scripting.JSObject;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public final class JSObjectMatchers {
	private JSObjectMatchers() {
	}

	public static Matcher<? super JSObject> jsArrayishContaining(Matcher<? super JSObject> elementMatcher) {
		return new TypeSafeDiagnosingMatcher<JSObject>() {
			@Override
			protected boolean matchesSafely(JSObject item, Description mismatchDescription) {
				if (item.values().isEmpty()) {
					mismatchDescription.appendText("empty");
					return false;
				}
				if (item.values().size() > 1) {
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

	public static Matcher<? super JSObject> jsArrayishContaining(Matcher<? super JSObject> firstElementMatcher,
			Matcher<? super JSObject> secondElementMatcher) {
		return new TypeSafeDiagnosingMatcher<JSObject>() {
			@Override
			protected boolean matchesSafely(JSObject item, Description mismatchDescription) {
				if (item.values().isEmpty()) {
					mismatchDescription.appendText("empty");
					return false;
				}
				if (item.values().size() != 2) {
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

	public static Matcher<? super JSObject> jsArray() {
		return new TypeSafeDiagnosingMatcher<JSObject>() {
			@Override
			protected boolean matchesSafely(JSObject item, Description mismatchDescription) {
				mismatchDescription.appendValue(item);
				return item.isArray();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("an array");
			}
		};
	}

	public static Matcher<? super JSObject> jsFunction() {
		return new TypeSafeDiagnosingMatcher<JSObject>() {
			@Override
			protected boolean matchesSafely(JSObject item, Description mismatchDescription) {
				mismatchDescription.appendValue(item);
				return item.isFunction();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("a function");
			}
		};
	}

	public static Matcher<? super JSObject> jsEmptyObject() {
		return new TypeSafeDiagnosingMatcher<JSObject>() {
			@Override
			protected boolean matchesSafely(JSObject item, Description mismatchDescription) {
				mismatchDescription.appendValue(item);
				return item.values().isEmpty();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("{}");
			}
		};
	}

}
