/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.LifecycleStateInjectorProcessor;
import org.mule.runtime.core.internal.registry.MuleContextProcessor;
import org.mule.runtime.core.internal.registry.SimpleRegistry;

public final class SimpleRegistryConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final SimpleRegistry registry =
        new SimpleRegistry(muleContext, ((DefaultMuleContext) muleContext).getLifecycleInterceptor());

    registry.registerObject(OBJECT_MULE_CONTEXT, muleContext);
    registry.registerObject(OBJECT_REGISTRY, new DefaultRegistry(muleContext));
    registry.registerObject("_muleContextProcessor", new MuleContextProcessor(muleContext));
    registry.registerObject(ErrorTypeRepository.class.getName(), createDefaultErrorTypeRepository());
    // processors.put("_registryProcessor", new RegistryProcessor(muleContext));
    registry.registerObject("_muleLifecycleStateInjectorProcessor",
                            new LifecycleStateInjectorProcessor(registry.getLifecycleManager().getState()));
    registry.registerObject("_muleLifecycleManager", registry.getLifecycleManager());

    ((DefaultMuleContext) muleContext).setRegistry(registry);
    ((DefaultMuleContext) muleContext).setInjector(registry);
  }
}
