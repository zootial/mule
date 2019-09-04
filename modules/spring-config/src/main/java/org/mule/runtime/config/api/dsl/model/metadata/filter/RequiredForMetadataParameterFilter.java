/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.filter;

import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;

/**
 * Handles filtering of {@link ParameterModel}s for computing a {@link MetadataCacheId}.
 *
 * @since 4.3.0
 */
public interface RequiredForMetadataParameterFilter {

  /**
   * Returns whether or not the given {@link ParameterModel} is required to obtain metadata.
   * @param parameterModel the model to check
   * @return {@link Boolean#TRUE} if the {@link ParameterModel} affects metadata computation
   */
  boolean isRequiredForMetadata(Object parameterModel);

}
