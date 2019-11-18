/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;

public class WhileTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();
  private While ws;

  public WhileTestCase() {
    setStartContext(true);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Override
  public void doTearDown() throws Exception {
    ws.stop();
    ws.dispose();
    super.doTearDown();
  }

  @After
  public void tearDown() throws MuleException {
    ws.stop();
  }

  private While createWhileProcessor(String expression, Processor... processors) throws MuleException {
    ws = new While();
    ws.setAnnotations(getAppleFlowComponentLocationAnnotations());
    final FlowConstruct flow = mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class));
    when(flow.getMuleContext()).thenReturn(muleContext);
    when(flow.getLocation()).thenReturn(TEST_CONNECTOR_LOCATION);
    ws.setMuleContext(muleContext);

    ws.setMessageProcessors(asList(processors));
    ws.setExpression(expression);
    muleContext.getInjector().inject(ws);
    ws.initialise();
    ws.start();

    return ws;
  }

  @Test
  public void doesNotExecute() throws Exception {
    TestProcessor processor = new TestProcessor(10);
    While ws = createWhileProcessor(While.DEFAULT_CONDITION_EXPRESSION, processor);
    ws.process(getEventBuilder().message(of(false)).build());
    assertThat(processor.getExecutionsLeft(), is(10));
  }

  @Test
  public void executeOnce() throws Exception {
    TestProcessor processor = new TestProcessor(1);
    While ws = createWhileProcessor(While.DEFAULT_CONDITION_EXPRESSION, processor);
    ws.process(getEventBuilder().message(of(true)).build());
    assertThat(processor.getExecutionsLeft(), is(0));
  }

  @Test
  public void executeMany() throws Exception {
    TestProcessor processor = new TestProcessor(10);
    While ws = createWhileProcessor(While.DEFAULT_CONDITION_EXPRESSION, processor);
    ws.process(getEventBuilder().message(of(true)).build());
    assertThat(processor.getExecutionsLeft(), is(0));
  }

  @Test
  public void executeMinBetweenProcessors() throws Exception {
    TestProcessor processor1 = new TestProcessor(3);
    TestProcessor processor2 = new TestProcessor(10);
    While ws = createWhileProcessor(While.DEFAULT_CONDITION_EXPRESSION, processor1, processor2);
    ws.process(getEventBuilder().message(of(true)).build());
    assertThat(processor1.getExecutionsLeft(), is(0));
    assertThat(processor2.getExecutionsLeft(), is(7));
  }

  @Test
  public void executeWithErrors() throws Exception {
    TestProcessor processor1 = new TestProcessor(3);
    TestProcessor processor2 = new TestProcessor(10, true);
    While ws = createWhileProcessor(While.DEFAULT_CONDITION_EXPRESSION, processor1, processor2);
    expected.expectCause(instanceOf(RuntimeException.class));
    ws.process(getEventBuilder().message(of(true)).build());
    assertThat(processor1.getExecutionsLeft(), is(2));
    assertThat(processor1.getExecutionsLeft(), is(9));
  }

  private static class TestProcessor implements Processor {

    private int executionsLeft;
    private boolean raiseError;

    TestProcessor(int executions, boolean raiseError) {
      this.executionsLeft = executions;
      this.raiseError = raiseError;
    }

    TestProcessor(int executions) {
      this(executions, false);
    }

    int getExecutionsLeft() {
      return executionsLeft;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      executionsLeft--;
      if (raiseError) {
        throw new RuntimeException("simulated problem");
      }
      if (executionsLeft > 0) {
        return event;
      } else {
        return CoreEvent.builder(event).message(of(false)).build();
      }
    }
  }
}
