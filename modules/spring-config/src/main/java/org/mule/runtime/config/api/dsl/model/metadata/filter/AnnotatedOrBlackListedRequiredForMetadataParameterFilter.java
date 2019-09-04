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

import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

/**
 * {@link RequiredForMetadataParameterFilter} that filters through a blacklist if no {@link ParameterModel} is annotated
 * with {@link RequiredForMetadata}
 *
 * @since 4.3.0
 */
public class AnnotatedOrBlackListedRequiredForMetadataParameterFilter implements RequiredForMetadataParameterFilter {

  private static final Set<String> BLACKLIST =
      new ImmutableSet.Builder<String>()
          .add("tlsContext")
          .add("name")
          .add("timeoutUnit")
          .add("timeout")
          .add("proxy")
          .add("encoding")
          .add("charset")
          .add("password")
          .build();

  private List<String> parameterList;

  public AnnotatedOrBlackListedRequiredForMetadataParameterFilter(Object elementModel) {
    if (elementModel instanceof EnrichableModel) {
      EnrichableModel enrichableModel = (EnrichableModel) elementModel;
      this.parameterList = enrichableModel
          .getModelProperty(RequiredForMetadataModelProperty.class)
          .map(RequiredForMetadataModelProperty::getRequiredParameters)
          .orElse(null);
    }
  }

  @Override
  public boolean isRequiredForMetadata(Object model) {
    if (model instanceof ParameterModel) {
      ParameterModel parameterModel = (ParameterModel) model;
      if (parameterList != null) {
        return parameterList.contains(parameterModel.getName());
      } else {
        return !BLACKLIST.contains(parameterModel.getName());
      }
    }
    return true;
  }

}
