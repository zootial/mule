/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional.app;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.functional.AbstractXmlExtensionMuleArtifactFunctionalTestCase;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SourceTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort httpPort1 = new DynamicPort("httpPort1");

  @Inject
  private ConfigurationComponentLocator componentLocator;

  private TestConnectorQueueHandler queueHandler;

  @Override
  protected String getConfigFile() {
    return "apps/source-config.xml";
  }

  @Before
  public void createQueueHandler() {
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  public void simpleTest() throws Exception {
    flowRunner("flowUsingOperation").run();
  }

}
