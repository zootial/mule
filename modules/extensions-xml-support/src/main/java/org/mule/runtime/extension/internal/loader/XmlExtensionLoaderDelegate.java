/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import java.util.List;
import java.util.Optional;

import org.mule.runtime.api.meta.model.ExtensionModel;

/**
 * Describes an {@link ExtensionModel} by scanning an XML provided in the constructor
 *
 * @since 4.0
 */
public class XmlExtensionLoaderDelegate extends AbstractXmlExtensionLoaderDelegate {


  /**
   * @param moduleFile         relative path to a file that will be loaded from the current {@link ClassLoader}. Non null.
   * @param validateXml        true if the XML of the Smart Connector must ve valid, false otherwise. It will be false at runtime, as the
   *                           packaging of a connector will previously validate it's XML.
   * @param declarationPath    relative path to a file that contains the {@link MetadataType}s of all <operations/>.
   * @param resourcesPaths     set of resources that will be exported in the {@link ExtensionModel}
   */
  public XmlExtensionLoaderDelegate(String moduleFile, boolean validateXml, Optional<String> declarationPath,
                                    List<String> resourcesPaths) {
    super(new String[] {moduleFile}, validateXml, declarationPath, resourcesPaths);
  }
}
