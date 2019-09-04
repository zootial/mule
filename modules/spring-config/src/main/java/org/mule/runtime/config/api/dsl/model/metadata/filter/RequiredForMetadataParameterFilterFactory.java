/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.filter;

import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;

/**
 * Returns a {@link RequiredForMetadataParameterFilter} that can provide information for every {@link ParameterModel}
 * involved in a {@link MetadataCacheId} computation.
 *
 * @since 4.3.0
 */
public interface RequiredForMetadataParameterFilterFactory {

  /**
   * Returns a new {@link RequiredForMetadataParameterFilter} that can handle parameters belonging to this {@param elementModel}
   * @param elementModel the {@link DslElementModel} that contains the paramters to filter
   * @return a new {@link RequiredForMetadataParameterFilter}
   */
  RequiredForMetadataParameterFilter createFilter(Object elementModel);

}
