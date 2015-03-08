package org.araqnid.testbed.jreact;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;

public class AppConfigModule extends AbstractModule {
	private static final Logger LOG = LoggerFactory.getLogger(AppConfigModule.class);
	private final Map<String, String> environment;

	public AppConfigModule(Map<String, String> environment) {
		this.environment = ImmutableMap.copyOf(environment);
	}

	@Override
	protected void configure() {
		bindConstant().annotatedWith(JettyModule.Port.class).to(getenv("PORT").orElse("61030"));
		install(new JettyModule());
	}

	@Provides
	@Version
	public String appVersion() {
		return Optional.ofNullable(AppConfigModule.class.getPackage().getImplementationVersion()).orElse("0.0.0");
	}

	@Provides
	@Singleton
	public ServiceManager serviceManager(@Managed Set<Service> services, @Version String appVersion) {
		ServiceManager serviceManager = new ServiceManager(services);
		serviceManager.addListener(new ServiceManager.Listener() {
			@Override
			public void stopped() {
			}

			@Override
			public void healthy() {
				LOG.info("Started {}", appVersion);
			}

			@Override
			public void failure(Service service) {
				System.exit(1);
			}
		}, MoreExecutors.directExecutor());
		return serviceManager;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
	@BindingAnnotation
	@Documented
	public @interface Version {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
	@BindingAnnotation
	@Documented
	public @interface Managed {
	}

	private Optional<String> getenv(String key) {
		return Optional.ofNullable(environment.get(key));
	}
}
