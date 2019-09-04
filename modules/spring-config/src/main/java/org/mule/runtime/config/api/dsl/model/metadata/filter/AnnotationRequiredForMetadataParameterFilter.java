/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.filter;

import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.metadata.RequiredForMetadata;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;

import java.util.List;

/**
 * {@link RequiredForMetadataParameterFilter} that checks if the {@link ParameterModel} is annotated with {@link RequiredForMetadata}.
 *
 * If none is annotated, then all will be required.
 *
 * @since 4.3.0
 */
public class AnnotationRequiredForMetadataParameterFilter implements RequiredForMetadataParameterFilter {

  private List<String> parameterList;

  public AnnotationRequiredForMetadataParameterFilter(Object elementModel) {
    if (elementModel instanceof EnrichableModel) {
      EnrichableModel enrichableModel = (EnrichableModel) elementModel;
      this.parameterList = enrichableModel
          .getModelProperty(RequiredForMetadataModelProperty.class)
          .map(RequiredForMetadataModelProperty::getRequiredParameters)
          .orElse(null);
    }
  }

  @Override
  public boolean isRequiredForMetadata(Object parameterModel) {
    if (parameterModel instanceof ParameterModel && parameterList != null) {
      return parameterList.contains(((ParameterModel) parameterModel).getName());
    }
    return true;
  }
}
