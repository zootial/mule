/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal;

import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.tck.TestBaseDeclarer;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;

/**
 * A simple pojo containing reference information for making test around a {@link ExtensionDeclarer}
 * which represents a theoretical &quot;Web Service Consumer&quot; extension.
 * <p>
 * It contains an actual {@link ExtensionDeclarer} that can be accessed through the {@link #getExtensionDeclarer()}
 * method plus some other getters which provides access to other declaration components
 * that you might want to make tests against.
 * <p>
 * This case focuses on the scenario in which all sources, providers and operations are available
 * on all configs
 *
 * @since 1.0
 */
public class TestToolingExtensionDeclarer extends TestBaseDeclarer {

  public static final String CONFIG_NAME = "config";
  public static final String CONFIG_DESCRIPTION = "Default description";
  public static final String EXTENSION_NAME = "SDKToolingExtension";
  public static final String EXTENSION_DESCRIPTION = "Fake Extension Model for SDK tooling tests";
  public static final String VERSION = "3.6.0";
  public static final String MULESOFT = "MuleSoft";

  public static final String USERNAME = "username";
  public static final String USERNAME_DESCRIPTION = "Authentication username";

  public static final String PASSWORD = "password";
  public static final String PASSWORD_DESCRIPTION = "Authentication password";

  public static final String HOST= "host";
  public static final String HOST_DESCRIPTION = "connection host";
  public static final String DEFAULT_HOST = "localhost";

  public static final String PORT = "port";
  public static final String PORT_DESCRIPTION = "The connection port";

  public static final String CONNECTION_PROVIDER_NAME = "connection";
  public static final String CONNECTION_PROVIDER_DESCRIPTION = "my connection provider";

  private ConnectionProviderFactory connectionProviderFactory;

  public ExtensionDeclarer declareOn(ExtensionDeclarer extensionDeclarer) {
    extensionDeclarer.named(EXTENSION_NAME)
        .describedAs(EXTENSION_DESCRIPTION)
        .onVersion(VERSION)
        .fromVendor(MULESOFT)
        .withCategory(SELECT)
        .withXmlDsl(XmlDslModel.builder().build());

    ConfigurationDeclarer config =
        extensionDeclarer.withConfig(CONFIG_NAME)
            .describedAs(CONFIG_DESCRIPTION);

    ConnectionProviderDeclarer connectionProvider =
        config.withConnectionProvider(CONNECTION_PROVIDER_NAME)
            .describedAs(CONNECTION_PROVIDER_DESCRIPTION)
            .withConnectionManagementType(NONE);

    if (connectionProviderFactory != null) {
      connectionProvider.withModelProperty(new ConnectionProviderFactoryModelProperty(connectionProviderFactory));
    }

    ParameterGroupDeclarer parameterGroup = connectionProvider.onParameterGroup(DEFAULT_GROUP_NAME);
    parameterGroup.withRequiredParameter(USERNAME).describedAs(USERNAME_DESCRIPTION).ofType(getStringType());
    parameterGroup.withRequiredParameter(PASSWORD).describedAs(PASSWORD_DESCRIPTION).ofType(getStringType());
    parameterGroup.withOptionalParameter(HOST).describedAs(HOST_DESCRIPTION).ofType(getStringType()).defaultingTo(DEFAULT_HOST);
    parameterGroup.withRequiredParameter(PORT).describedAs(PORT_DESCRIPTION).ofType(getNumberType());

    return extensionDeclarer;
  }

  public ExtensionDeclarer getExtensionDeclarer() {
    return declareOn(new ExtensionDeclarer());
  }

  public void setConnectionProviderFactory(ConnectionProviderFactory connectionProviderFactory) {
    this.connectionProviderFactory = connectionProviderFactory;
  }
}
