/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.ExtensionTypeElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Method Factory Pattern implementation that creates {@link ExtensionElement} from {@link Class} or {@link TypeElement}
 *
 * @since 4.0
 */
public class ExtensionTypeFactory {

  /**
   * Creates a {@link ExtensionElement} from a given {@link Class} that will help to introspect an extension.
   *
   * @param extensionClass {@link Extension} annotated {@link Class}
   * @return an {@link ExtensionElement} wrapping the extension {@link Class} structure
   */
  public static ExtensionElement getExtensionType(Class<?> extensionClass, ClassTypeLoader classTypeLoader) {
    return new ExtensionTypeWrapper<>(extensionClass, classTypeLoader);
  }

  /**
   * Creates a {@link ExtensionElement} from a given {@link Class} that will help to introspect an extension.
   *
   * @param extensionElement {@link Extension} annotated {@link Class}
   * @return an {@link ExtensionElement} wrapping the extension {@link Class} structure
   */
  public static ExtensionElement getExtensionType(TypeElement extensionElement, ProcessingEnvironment processingEnvironment) {
    return new ExtensionTypeElement(extensionElement, processingEnvironment);
  }
}