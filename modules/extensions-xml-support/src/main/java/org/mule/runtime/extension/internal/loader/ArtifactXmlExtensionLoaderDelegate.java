/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;

/**
 * Describes an {@link ExtensionModel} by scanning an XML provided in the constructor
 *
 * @since 4.0
 */
public final class ArtifactXmlExtensionLoaderDelegate extends AbstractXmlExtensionLoaderDelegate {

  private final String name;
  //TODO PLG this is not working because artifactType is being used before assigned
  private ArtifactType artifactType = ArtifactType.APP;

  /**
   * @param configurationFiles relative path to a file that will be loaded from the current {@link ClassLoader}. Non null.
   * @param validateXml        true if the XML of the Smart Connector must ve valid, false otherwise. It will be false at runtime, as the
   * @param artifactType
   */
  public ArtifactXmlExtensionLoaderDelegate(String[] configurationFiles, boolean validateXml, String name,
                                            ArtifactType artifactType) {
    super(configurationFiles, validateXml, empty(), Collections.emptyList());
    this.name = name;
    this.artifactType = artifactType;
  }

  @Override
  protected String getCustomTypeFilename() {
    //TODO PLG implement default custom types file name
    return super.getCustomTypeFilename();
  }

  @Override
  public String getModuleNamespaceName() {
    return "mule";
    //return artifactType.equals(ArtifactType.APP) ? "mule" : "mule-domain";
  }

  @Override
  protected String getTransformationForTnsResource() {
    return "META-INF/transform_for_tns_mule.xsl";
  }

  @Override
  protected String getCategory(ComponentModel moduleModel) {
    return Category.COMMUNITY.name();
  }

  @Override
  protected Optional<String> getModuleName(ComponentModel moduleModel) {
    return empty();
  }

  @Override
  protected String getName(ComponentModel moduleModel) {
    return name;
  }

  @Override
  protected String getVendor(ComponentModel moduleModel) {
    return "custom";
  }

  @Override
  protected void addGlobalElementModelProperty(ExtensionDeclarer declarer, List<ComponentModel> globalElementsComponentModel) {
    //There are not global elements in artifacts at least for now
  }
}
