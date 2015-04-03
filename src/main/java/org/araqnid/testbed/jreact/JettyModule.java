package org.araqnid.testbed.jreact;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.araqnid.testbed.jreact.AppConfigModule.Managed;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;

import com.google.common.reflect.ClassPath;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

public class JettyModule extends AbstractModule {
	@Override
	protected void configure() {
		managedServices().addBinding().to(JettyService.class);
	}

	@Provides
	@Singleton
	public Server jettyServer(@Port int port, Handler handler) {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		server.setHandler(handler);
		return server;
	}

	@Provides
	public Handler handler(@Named("webappRoot") Resource webappRoot) {
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.setBaseResource(webappRoot);
		servletContextHandler.addServlet(DefaultServlet.class, "/*");
		return servletContextHandler;
	}

	@Provides
	@Named("webappRoot")
	@Singleton
	public Resource webappRoot() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		return new EmbeddedResource(classLoader, "web", ClassPath.from(classLoader));
	}

	private Multibinder<Service> managedServices() {
		return Multibinder.newSetBinder(binder(), Service.class, Managed.class);
	}

	public static class JettyService extends AbstractIdleService {
		private final Server server;

		@Inject
		public JettyService(Server server) {
			this.server = server;
		}

		@Override
		protected void startUp() throws Exception {
			server.start();
		}

		@Override
		protected void shutDown() throws Exception {
			server.stop();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
	@Qualifier
	@Documented
	public @interface Port {
	}
}
