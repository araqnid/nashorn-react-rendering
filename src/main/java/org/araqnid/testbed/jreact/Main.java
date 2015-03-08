package org.araqnid.testbed.jreact;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
	private static final ThreadFactory shutdownThreadFactory = new ThreadFactoryBuilder().setNameFormat("shutdown")
			.build();

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new AppConfigModule(System.getenv()));
		ServiceManager serviceManager = injector.getInstance(ServiceManager.class);
		serviceManager.startAsync().awaitHealthy();
		Runtime.getRuntime().addShutdownHook(shutdownThreadFactory.newThread(() -> {
			try {
				serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
			} catch (TimeoutException timeout) {
				// stopping timed out
			}
		}));
	}
}
