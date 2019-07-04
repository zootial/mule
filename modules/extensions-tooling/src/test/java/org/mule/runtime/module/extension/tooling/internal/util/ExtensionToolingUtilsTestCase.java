/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class ExtensionToolingUtilsTestCase extends AbstractMuleTestCase {


  @Test
  public void createMuleContext() throws Exception {
    MuleContext muleContext = ExtensionsToolingUtils.createMuleContext(true);

    assertThat(muleContext.isInitialised(), is(true));
    assertThat(muleContext.isStarted(), is(true));
  }




}
