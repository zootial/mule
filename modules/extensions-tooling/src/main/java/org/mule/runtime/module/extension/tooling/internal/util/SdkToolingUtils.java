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

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;

import java.util.Map;

import org.slf4j.Logger;

public final class SdkToolingUtils {

  private static final Logger LOGGER = getLogger(SdkToolingUtils.class);

  private SdkToolingUtils() {
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

  public static void stopAndDispose(Object object) {
    if (object == null) {
      return;
    }
    
    try {
      stopIfNeeded(object);
    } catch (MuleException e) {
      LOGGER.error("Exception trying to stop " + object, e);
    } finally {
      disposeIfNeeded(object, LOGGER);
    }
  }
}
