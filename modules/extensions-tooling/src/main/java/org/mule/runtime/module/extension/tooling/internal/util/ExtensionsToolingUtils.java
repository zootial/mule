/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal.util;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.tooling.internal.service.scheduler.ToolingSchedulerService;

import java.util.Map;

import org.slf4j.Logger;

public final class ExtensionsToolingUtils {

  private static final Logger LOGGER = getLogger(ExtensionsToolingUtils.class);

  private ExtensionsToolingUtils() {
  }

  public static ResolverSet toResolverSet(Map<String, ?> values, MuleContext muleContext) {
    ResolverSet resolverSet = new ResolverSet(muleContext);
    values.forEach((k, v) -> resolverSet.add(k, new StaticValueResolver(v)));

    return resolverSet;
  }

  public static ResolverSetResult toResolverSetResult(Map<String, ?> values) {
    ResolverSetResult.Builder builder = ResolverSetResult.newBuilder();
    values.forEach(builder::add);

    return builder.build();
  }

  public static <T> void stopAndDispose(ConnectionProvider<T> connectionProvider) {
    try {
      stopIfNeeded(connectionProvider);
    } catch (MuleException e) {
      LOGGER.error("Exception trying to stop connection provider", e);
    } finally {
      disposeIfNeeded(connectionProvider, LOGGER);
    }
  }

  public static MuleContext createMuleContext(boolean start) throws MuleException {
    MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext(getConfigurationBuilders());

    if (start) {
      //muleContext.initialise();
      muleContext.start();
    }

    return muleContext;
  }

  private static ConfigurationBuilder[] getConfigurationBuilders() {
    return new ConfigurationBuilder[] {
        new DefaultsConfigurationBuilder(),
        getServicesConfigurationBuilder(),
        getInjectionConfigurationBuilder()
    };
  }

  private static ConfigurationBuilder getInjectionConfigurationBuilder() {
    return new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        DefaultMuleContext ctx = (DefaultMuleContext) muleContext;
        Injector injector = ctx.getInjector();

        ctx.getRegistry().lookupObjects(Object.class).forEach((CheckedConsumer<Object>) injector::inject);
      }
    };
  }

  private static ConfigurationBuilder getServicesConfigurationBuilder() {
    return new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) {
        MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
        SchedulerService schedulerService = new ToolingSchedulerService();
        try {
          registry.registerObject(getServiceId(SchedulerService.class, schedulerService), schedulerService);
        } catch (RegistrationException e) {
          throw new MuleRuntimeException(e);
        }
      }
    };
  }

  private static <T extends Service> String getServiceId(Class<T> contractClass, T service) {
    return service.getName() + "-" + contractClass.getSimpleName();
  }
}
