package org.araqnid.testbed.jreact;

import java.io.IOException;

import org.junit.Test;

import com.google.common.reflect.ClassPath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;

public class EmbeddedResourceTest {
	@Test
	public void resource_for_root_exists() throws Exception {
		EmbeddedResource resource = webappRoot();
		assertThat(resource.exists(), equalTo(true));
	}

	@Test
	public void resource_for_root_is_a_directory() throws Exception {
		EmbeddedResource resource = webappRoot();
		assertThat(resource.isDirectory(), equalTo(true));
	}

	@Test
	public void resource_for_root_contains_index() throws Exception {
		EmbeddedResource resource = webappRoot();
		assertThat(resource.list(), hasItemInArray("index.html"));
	}

	private EmbeddedResource webappRoot() throws IOException {
		return new EmbeddedResource(getClass().getClassLoader(), "web", ClassPath.from(getClass()
				.getClassLoader()));
	}
}
