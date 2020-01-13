/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Flux;

/**
 * Decorates a {@link ProcessingStrategy} so that processing takes place on the current thread in the event of a transaction being
 * active.
 *
 * @since 4.3.0
 */
public class TransactionAwareStreamEmitterProcessingStrategyDecorator extends ProcessingStrategyDecorator {

  private static final Consumer<CoreEvent> NULL_EVENT_CONSUMER = event -> {
  };

  public TransactionAwareStreamEmitterProcessingStrategyDecorator(ProcessingStrategy delegate) {
    super(delegate);
    if (delegate instanceof ProcessingStrategyAdapter) {
      ProcessingStrategyAdapter adapter = (ProcessingStrategyAdapter) delegate;

      adapter.setOnEventConsumer(NULL_EVENT_CONSUMER);
      Function<ScheduledExecutorService, ScheduledExecutorService> delegateDecorator = adapter.getSchedulerDecorator();
      adapter.setSchedulerDecorator(scheduler -> new ConditionalExecutorServiceDecorator(delegateDecorator.apply(scheduler),
                                                                                         currentScheduler -> isTransactionActive()));
    }
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    Sink delegateSink = delegate.createSink(flowConstruct, pipeline);
    Sink syncSink = new StreamPerThreadSink(pipeline, NULL_EVENT_CONSUMER, flowConstruct);
    return new TransactionalDelegateSink(syncSink, delegateSink);
  }

  @Override
  public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
    return isTransactionActive()
        ? BLOCKING_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline)
        : delegate.onPipeline(pipeline);
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    if (processor.getProcessingType() == CPU_LITE_ASYNC) {
      final FluxSinkRecorder<CoreEvent> txEmitter = new FluxSinkRecorder<>();
      final Flux<CoreEvent> txFlux = Flux.create(txEmitter)
          .transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor));

      return publisher -> Flux.from(publisher)
          .doOnNext(event -> {
            if (isTransactionActive()) {
              txEmitter.next(event);
            }
          })

          .filter(event -> !isTransactionActive())
          .transform(delegate.onProcessor(processor))
          .doOnComplete(() -> {
            // System.out.println(" >> completing...");
            txEmitter.complete();
          })
          .mergeWith(txFlux);

    } else {
      // The conditional schedulers will take care of avoiding thread switches
      return delegate.onProcessor(processor);
    }

    // if (isTransactionActive() && processor.getProcessingType() == CPU_LITE_ASYNC) {
    // return BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor);
    // } else {
    // return delegate.onProcessor(processor);
    // }
  }
}
