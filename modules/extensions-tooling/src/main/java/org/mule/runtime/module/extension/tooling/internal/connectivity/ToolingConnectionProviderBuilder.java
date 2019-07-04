/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal.connectivity;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

public class ToolingConnectionProviderBuilder extends ConnectionProviderObjectBuilder<Object> {


  public ToolingConnectionProviderBuilder(ConnectionProviderModel providerModel,
                                          ResolverSet resolverSet,
                                          ExtensionModel extensionModel,
                                          ExpressionManager expressionManager,
                                          MuleContext muleContext) {
    super(providerModel, resolverSet, extensionModel, expressionManager, muleContext);
    setOwnerConfigName("Connectivity Test");
  }
}
