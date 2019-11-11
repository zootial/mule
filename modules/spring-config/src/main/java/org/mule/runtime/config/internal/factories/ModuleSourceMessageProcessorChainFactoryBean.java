/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.lang.String.format;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.newLazyProcessorChainBuilder;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.source.SchedulerConfiguration;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.PrivateOperationsModelProperty;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.MessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.objectfactory.MessageProcessorChainObjectFactory;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;

public class ModuleSourceMessageProcessorChainFactoryBean extends AbstractComponentFactory<MessageSource> {

  private Map<String, String> properties = new HashMap<>();
  private Map<String, String> parameters = new HashMap<>();
  private String moduleName;
  private String moduleSource;
  private MessageSource source;
  @Inject
  private ExtensionManager extensionManager;

  @Inject
  protected ConfigurationComponentLocator locator;

  @Inject
  private Registry registry;

  @Inject
  private MuleContext muleContext;

  private List<Processor> processors;

  @Override
  public MessageSource doGetObject() throws Exception {
    Optional<ProcessingStrategy> processingStrategy = getProcessingStrategy(locator, getRootContainerLocation());
    MessageProcessorChain nestedChain = buildNewChainWithListOfProcessors(processingStrategy, processors);
    if (source instanceof SchedulerMessageSource) {
      return new InternalScheduledMessageSource(source, nestedChain, registry);
    }
    return new InternalMessageSource(source, nestedChain, registry);
  }

  private ExtensionModel getExtensionModelOrFail() {
    return extensionManager.getExtensions().stream()
        .filter(em -> em.getXmlDslModel().getPrefix().equals(moduleName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("Could not find any extension under the name of [%s]",
                                                               moduleName)));
  }

  private OperationModel getOperationModelOrFail(ExtensionModel extensionModel) {
    OperationSeeker operationSeeker = new OperationSeeker();
    operationSeeker.walk(extensionModel);
    final OperationModel operationModel =
        operationSeeker.operationModel
            .orElseGet(
                       () -> extensionModel.getModelProperty(PrivateOperationsModelProperty.class).get()
                           .getOperationModel(moduleSource)
                           .orElseThrow(() -> new IllegalArgumentException(format("Could not find any source under the name of [%s] for the extension [%s]",
                                                                                  moduleSource, moduleName))));
    return operationModel;
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

  public void setSource(MessageSource source) {
    this.source = source;
  }

  /**
   * Internal class used only as a helper to find the only occurrence of an operation under the same name.
   */
  private class OperationSeeker extends IdempotentExtensionWalker {

    Optional<OperationModel> operationModel = Optional.empty();

    @Override
    protected void onOperation(OperationModel operationModel) {
      if (operationModel.getName().equals(moduleSource)) {
        this.operationModel = Optional.of(operationModel);
        stop();
      }
    }
  }

  public static class InternalScheduledMessageSource extends InternalMessageSource implements SchedulerMessageSource {

    public InternalScheduledMessageSource(MessageSource source, MessageProcessorChain messageProcessorChain, Registry registry) {
      super(source, messageProcessorChain, registry);
    }

    @Override
    public void trigger() {
      ((SchedulerMessageSource) source).trigger();
    }

    @Override
    public boolean isStarted() {
      return ((SchedulerMessageSource) source).isStarted();
    }

    @Override
    public SchedulerConfiguration getConfiguration() {
      return ((SchedulerMessageSource) source).getConfiguration();
    }

  }

  public static class InternalMessageSource extends AbstractMuleObjectOwner implements MessageSource {

    private final MessageProcessorChain messageProcessorChain;
    private final Registry registry;
    protected final MessageSource source;
    private Processor listener;

    public InternalMessageSource(MessageSource source, MessageProcessorChain messageProcessorChain, Registry registry) {
      this.source = source;
      this.messageProcessorChain = messageProcessorChain;
      this.registry = registry;
    }

    @Override
    public void setListener(Processor listener) {
      this.listener = listener;
    }

    @Override
    protected List getOwnedObjects() {
      return ImmutableList.of(source, listener);
    }

    @Override
    public void initialise() throws InitialisationException {
      // TODO PLG review implementation
      try {
        registry.lookupByType(MuleContext.class).get().getInjector().inject(source);
      } catch (MuleException e) {
        throw new InitialisationException(e, this);
      }
      source.setListener(new Processor() {

        @Override
        public CoreEvent process(CoreEvent event) throws MuleException {
          return processToApply(event, this);
        }

        @Override
        public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
          return from(publisher)
              .transform(t -> MessageProcessors.applyWithChildContext(t, messageProcessorChain, Optional.of(getLocation())))
              .transform(listener);
        }
      });
      super.initialise();
      LifecycleUtils.initialiseIfNeeded(source);
    }

    @Override
    public BackPressureStrategy getBackPressureStrategy() {
      return source.getBackPressureStrategy();
    }
  }
}
