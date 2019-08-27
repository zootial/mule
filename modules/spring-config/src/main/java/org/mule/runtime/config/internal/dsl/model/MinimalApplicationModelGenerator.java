/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.mule.runtime.ast.graph.api.ArtifactAstGraphFactory.generateFor;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;

import java.util.function.Predicate;

/**
 * Generates the minimal required component set to create a configuration component (i.e.: file:config, ftp:connection, a flow
 * MP). This set is defined by the component dependencies.
 * <p/>
 * Based on the requested component, the {@link ComponentModel} configuration associated is introspected to find it dependencies
 * based on it's {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}. This process is recursively done for each
 * of the dependencies in order to find all the required {@link ComponentModel}s that must be created for the requested
 * {@link ComponentModel} to work properly.
 *
 * @since 4.0
 */
// TODO MULE-9688 - refactor this class when the ComponentModel becomes immutable
public class MinimalApplicationModelGenerator {

  private final ConfigurationDependencyResolver dependencyResolver;
  private boolean ignoreAlwaysEnabled = false;

  /**
   * Creates a new instance.
   *
   * @param dependencyResolver a {@link ConfigurationDependencyResolver} associated with an {@link ApplicationModel}
   */
  public MinimalApplicationModelGenerator(ConfigurationDependencyResolver dependencyResolver) {
    this(dependencyResolver, false);
  }

  /**
   * Creates a new instance of the minimal application generator.
   *
   * @param dependencyResolver a {@link ConfigurationDependencyResolver} associated with an {@link ApplicationModel}
   * @param ignoreAlwaysEnabled {@code true} if consider those components that will not be referenced and have to be enabled
   *        anyways.
   */
  public MinimalApplicationModelGenerator(ConfigurationDependencyResolver dependencyResolver, boolean ignoreAlwaysEnabled) {
    this.dependencyResolver = dependencyResolver;
    this.ignoreAlwaysEnabled = ignoreAlwaysEnabled;
  }

  /**
   * Resolves the minimal set of {@link ComponentModel componentModels} for the components that pass the
   * {@link LazyComponentInitializer.ComponentLocationFilter}.
   *
   * @param predicate to select the {@link ComponentModel componentModels} to be enabled.
   * @return the generated {@link ApplicationModel} with the minimal set of {@link ComponentModel}s required.
   */
  public ArtifactAst getMinimalModel(Predicate<ComponentAst> predicate) {
    return generateFor(dependencyResolver.getApplicationModel())
        .minimalArtifactFor(predicate
            .or(componentModel -> componentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER))
            .or(comp -> !ignoreAlwaysEnabled
                && ("spring".equals(comp.getIdentifier().getNamespace())
                    && ("config".equals(comp.getIdentifier().getName())
                        || "security-manager".equals(comp.getIdentifier().getName())))));
  }

}
