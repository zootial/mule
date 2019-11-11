/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.source.SchedulerConfiguration;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceComponent extends AbstractMessageProcessorOwner implements MessageSource, SchedulerMessageSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceComponent.class);
  private Map<String, String> properties = new HashMap<>();
  private Map<String, String> parameters = new HashMap<>();
  private String moduleName;
  private String moduleSource;
  private Processor listener;
  private MessageProcessorChain nestedChain;
  private SourceBody body;
  @Inject
  private Registry registry;

  public void setBody(SourceBody sourceBody) {
    this.body = sourceBody;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return Collections.singletonList(nestedChain);
  }

  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }

  public void setModuleSource(String moduleSource) {
    this.moduleSource = moduleSource;
  }

  @Override
  public BackPressureStrategy getBackPressureStrategy() {
    return body.getSource().getBackPressureStrategy();
  }

  @Override
  public void initialise() throws InitialisationException {
    // TODO PLG review implementation
    try {
      registry.lookupByType(MuleContext.class).get().getInjector().inject(body.getSource());
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
    Optional<ProcessingStrategy> processingStrategy = getProcessingStrategy(locator, getRootContainerLocation());
    nestedChain = buildNewChainWithListOfProcessors(processingStrategy, body.getMessageProcessors());
    body.getSource().setListener(new Processor() {

      @Override
      public CoreEvent process(CoreEvent event) throws MuleException {
        return processToApply(event, this);
      }

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        return from(publisher)
            .transform(t -> MessageProcessors.applyWithChildContext(t, nestedChain, Optional.of(getLocation())))
            .transform(listener);
      }
    });
    // source.setListener(event -> {
    // Mono<CoreEvent> mono = Mono.from(MessageProcessors.processWithChildContext(event, nestedChain,
    // Optional.of(getLocation())));
    //
    // });
    super.initialise();
    LifecycleUtils.initialiseIfNeeded(body.getSource());
  }

  @Override
  public void dispose() {
    LifecycleUtils.disposeIfNeeded(body.getSource(), LOGGER);
    super.dispose();
  }

  @Override
  public void start() throws MuleException {
    super.start();
    LifecycleUtils.startIfNeeded(body.getSource());
  }

  @Override
  public void stop() throws MuleException {
    LifecycleUtils.stopIfNeeded(body.getSource());
    super.stop();
  }

  @Override
  public void trigger() {
    getScheduledMessageSource().trigger();
  }

  @Override
  public boolean isStarted() {
    return getScheduledMessageSource().isStarted();
  }

  @Override
  public SchedulerConfiguration getConfiguration() {
    return getScheduledMessageSource().getConfiguration();
  }

  private SchedulerMessageSource getScheduledMessageSource() {
    // TODO PLG improve
    if (body.getSource() instanceof SchedulerMessageSource) {
      return (SchedulerMessageSource) body.getSource();
    }
    throw new IllegalStateException("source is not of type ScheduledMessageSource");
  }
}
