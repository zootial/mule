/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.api;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.module.extension.tooling.internal.util.ExtensionsToolingUtils.createMuleContext;
import static org.mule.runtime.module.extension.tooling.internal.util.ExtensionsToolingUtils.stopAndDispose;
import static org.mule.runtime.module.extension.tooling.internal.util.ExtensionsToolingUtils.toResolverSet;
import static org.mule.runtime.module.extension.tooling.internal.util.ExtensionsToolingUtils.toResolverSetResult;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.tooling.internal.ToolingExpressionManager;
import org.mule.runtime.module.extension.tooling.internal.connectivity.ToolingConnectionProviderBuilder;

import java.util.Map;

import org.slf4j.Logger;

public class ToolingConnectivityTester {

  private static final Logger LOGGER = getLogger(ToolingConnectivityTester.class);

  public ConnectionValidationResult testConnection(ExtensionModel extensionModel,
                                                   ConnectionProviderModel connectionProviderModel,
                                                   Map<String, Object> params) throws MuleException {


    ConnectionProvider<Object> connectionProvider = createConnectionProvider(extensionModel, connectionProviderModel, params);

    Object connection;
    try {
      connection = connectionProvider.connect();
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }

    try {
      return connectionProvider.validate(connection);
    } finally {
      stopAndDispose(connectionProvider);
    }
  }

  private ConnectionProvider<Object> createConnectionProvider(ExtensionModel extensionModel,
                                                              ConnectionProviderModel connectionProviderModel,
                                                              Map<String, Object> params) throws MuleException {

    final MuleContext muleContext = createMuleContext(true);
    final ExpressionManager expressionManager = new ToolingExpressionManager();

    ResolverSet resolverSet = toResolverSet(params, muleContext);
    ResolverSetResult result = toResolverSetResult(params);

    ConnectionProviderObjectBuilder<Object> objectBuilder = new ToolingConnectionProviderBuilder(connectionProviderModel,
                                                                                                 resolverSet,
                                                                                                 extensionModel,
                                                                                                 expressionManager,
                                                                                                 muleContext);

    ConnectionProvider<Object> connectionProvider;
    try {
      connectionProvider = objectBuilder.build(result).getFirst();
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }

    try {
      initialiseIfNeeded(connectionProvider, true, muleContext);
    } catch (InitialisationException e) {
      disposeIfNeeded(connectionProvider, LOGGER);
      throw e;
    }
    startIfNeeded(connectionProvider);

    return connectionProvider;
  }
}
