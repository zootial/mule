/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.api;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer.DEFAULT_HOST;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.module.extension.tooling.internal.DefaultSdkToolingExecutor;
import org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer;
import org.mule.runtime.module.extension.tooling.internal.service.scheduler.ToolingSchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestConnectivityTestCase extends AbstractMuleTestCase {

  public static final String HOST = "localhost";
  public static final String PASSWORD = "dietMule";
  public static final String USER = "dietuser";
  public static final String PORT = "8080"; // intentionally a String, to force value convertion

  private ExtensionModel extensionModel;
  private ConnectionProviderModel connectionProviderModel;

  private ClassLoader classLoader = getClass().getClassLoader();
  private TestConnectionProvider connectionProvider = new TestConnectionProvider();
  private SdkToolingExecutor executor = new DefaultSdkToolingExecutor();

  @Before
  public void setup() throws Exception {
    TestToolingExtensionDeclarer declarer = new TestToolingExtensionDeclarer();
    declarer.setConnectionProviderFactory(new ConnectionProviderFactory() {

      @Override
      public ConnectionProvider newInstance() {
        return connectionProvider;
      }

      @Override
      public Class<? extends ConnectionProvider> getObjectType() {
        return connectionProvider.getClass();
      }
    });

    extensionModel = new ExtensionModelFactory().create(
        new DefaultExtensionLoadingContext(declarer.getExtensionDeclarer(), classLoader,
                                           DslResolvingContext.getDefault(emptySet())));

    connectionProviderModel = extensionModel.getConfigurationModels().get(0).getConnectionProviders().get(0);
  }

  @Test
  public void testConnectivity() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("username", USER);
    params.put("password", PASSWORD);
    params.put("host", HOST);
    params.put("port", PORT); //TODO: need to use type safe ValueResolvers

    long now = System.currentTimeMillis();
    ConnectionValidationResult result = executor.testConnectivity(extensionModel, connectionProviderModel, classLoader, params);
    System.out.println("Diet Connectivity test took " + (System.currentTimeMillis() - now) + " ms");

    if (!result.isValid()) {
      if (result.getException() != null) {
        throw result.getException();
      }

      fail("Connectivity testing failed");
    }

    assertThat(result.getException(), is(nullValue()));
    assertThat(result.getErrorType().isPresent(), is(false));

    assertThat(connectionProvider.getInitialise(), is(1));
    assertThat(connectionProvider.getStart(), is(1));
    assertThat(connectionProvider.getStop(), is(1));
    assertThat(connectionProvider.getDispose(), is(1));

    assertThat(connectionProvider.getUsername(), equalTo(USER));
    assertThat(connectionProvider.getPassword(), equalTo(PASSWORD));
    assertThat(connectionProvider.getHost(), equalTo(HOST));
    assertThat(connectionProvider.getPort(), equalTo(Integer.parseInt(PORT)));

    assertThat(connectionProvider.getLockFactory(), is(instanceOf(MuleLockFactory.class)));
    assertThat(connectionProvider.getSchedulerService(), is(instanceOf(ToolingSchedulerService.class)));
    //TODO: Also test the config ref name;

    assertThat(connectionProvider.getConnection().isConnected(), is(false));
  }


  private class TestConnectionProvider implements ConnectionProvider<Connection>, Lifecycle {

    private Connection connection = new Connection();
    private int initialise, start, stop, dispose = 0;

    @Inject
    private LockFactory lockFactory;

    @RefName
    private String configName;

    @Inject
    private SchedulerService schedulerService;

    @Parameter
    private String username;

    @Parameter
    @Password
    private String password;

    @Parameter
    @Optional(defaultValue = DEFAULT_HOST)
    private String host;

    @Parameter
    private Integer port;

    @Override
    public Connection connect() throws ConnectionException {
      return connection;
    }

    @Override
    public void disconnect(Connection connection) {
      connection.setConnected(false);
    }

    @Override
    public ConnectionValidationResult validate(Connection connection) {
      if (connection.isConnected()) {
        return success();
      } else {
        return failure("Not connected", new IllegalStateException());
      }
    }

    @Override
    public void initialise() throws InitialisationException {
      initialise++;
    }

    @Override
    public void start() throws MuleException {
      start++;
    }

    @Override
    public void stop() throws MuleException {
      stop++;
    }

    @Override
    public void dispose() {
      dispose++;
    }

    public Connection getConnection() {
      return connection;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }

    public String getHost() {
      return host;
    }

    public Integer getPort() {
      return port;
    }

    public int getInitialise() {
      return initialise;
    }

    public int getStart() {
      return start;
    }

    public int getStop() {
      return stop;
    }

    public int getDispose() {
      return dispose;
    }

    public LockFactory getLockFactory() {
      return lockFactory;
    }

    public String getConfigName() {
      return configName;
    }

    public SchedulerService getSchedulerService() {
      return schedulerService;
    }
  }


  private class Connection {

    private boolean connected = true;

    public boolean isConnected() {
      return connected;
    }

    public void setConnected(boolean connected) {
      this.connected = connected;
    }
  }
}
