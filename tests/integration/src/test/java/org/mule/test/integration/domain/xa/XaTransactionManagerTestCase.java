/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.xa;

import org.hamcrest.core.IsInstanceOf;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ConfigurationException;
import org.mule.tck.junit4.DomainFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;


public class XaTransactionManagerTestCase extends DomainFunctionalTestCase
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Override
    public void setUpMuleContexts() throws Exception
    {
        thrown.expect(ConfigurationException.class);
        thrown.expect(hasMessage(containsString("No qualifying bean of type 'org.mule.api.transaction.TransactionManagerFactory' available: expected single matching bean but found 2:")));
//        thrown.expect(ThrowableRootCauseMatcher.hasRootCause(IsInstanceOf.<ConfigurationException> instanceOf(NoUniqueBeanDefinitionException.class)));
        super.setUpMuleContexts();
    }

    public static final String APPLICATION_NAME = "app";

    @Override
    protected String getDomainConfig()
    {
        return "domain/xa/jboss-ts-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                new ApplicationConfig(APPLICATION_NAME, new String[] {"domain/xa/app-with-tx-manager-config.xml"})
        };
    }

    @Test(expected = MuleRuntimeException.class)
    public void validateOnlyOneTxManagerCanBeUsed()
    {
         getMuleContextForApp(APPLICATION_NAME).getTransactionManager();
    }
}
