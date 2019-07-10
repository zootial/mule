/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal.util.bootstrap;

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
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.module.extension.tooling.internal.service.scheduler.ToolingSchedulerService;

public class ToolingMuleContextFactory {

  public MuleContext createMuleContext() throws MuleException {
    return createMuleContext(true);
  }

  public MuleContext createMuleContext(boolean start) throws MuleException {
    MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext(getConfigurationBuilders());

    if (start) {
      muleContext.start();
    }

    return muleContext;
  }

  private ConfigurationBuilder[] getConfigurationBuilders() {
    return new ConfigurationBuilder[] {
        new ToolingConfigurationBuilder(),
        getServicesConfigurationBuilder()
        //getInjectionConfigurationBuilder()
    };
  }

  private ConfigurationBuilder getInjectionConfigurationBuilder() {
    return new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) {
        DefaultMuleContext ctx = (DefaultMuleContext) muleContext;
        Injector injector = ctx.getInjector();

        ctx.getRegistry().lookupObjects(Object.class).forEach((CheckedConsumer<Object>) injector::inject);
      }
    };
  }

  private ConfigurationBuilder getServicesConfigurationBuilder() {
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

  private <T extends Service> String getServiceId(Class<T> contractClass, T service) {
    return service.getName() + "-" + contractClass.getSimpleName();
  }

}
