/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.*;

/**
 * javadoc needed
 */
public class While extends AbstractMessageProcessorOwner implements Initialisable, Scope {

  static final String DEFAULT_CONDITION_EXPRESSION = "#[payload]";

  @Inject
  private ExpressionManager expressionManager;

  private List<Processor> messageProcessors;
  private String expression = DEFAULT_CONDITION_EXPRESSION;
  private MessageProcessorChain nestedChain;

  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(nestedChain);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    Predicate<CoreEvent> iterateAgain = event -> expressionManager.evaluateBoolean(expression, event, this.getLocation());
    return new WhileRouter(this, publisher, nestedChain, iterateAgain).getDownstreamPublisher();
  }

  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public void initialise() throws InitialisationException {
    Optional<ProcessingStrategy> processingStrategy = getProcessingStrategy(locator, getRootContainerLocation());
    nestedChain = buildNewChainWithListOfProcessors(processingStrategy, messageProcessors);
    super.initialise();
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }
}
