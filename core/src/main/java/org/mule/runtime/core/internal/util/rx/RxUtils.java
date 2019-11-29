/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.create;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Reactor specific utils
 */
public class RxUtils {

  /**
   * Defers the subscription of the <it>deferredSubscriber</it> until <it>triggeringSubscriber</it> subscribes. Once that occurs
   * the latter subscription will take place on the same context. For an example of this, look at
   * {@link org.mule.runtime.core.internal.routing.ChoiceRouter}
   * <p>
   * This serves its purpose in some in which the are two Fluxes, A and B, and are related in that in some part of A's reactor
   * chain, the processed event is published into a sink that belongs to B. Also, suppose that some of A's processors need to be
   * initialized in order to make the whole assembled chain work. In those cases, one may want to do A's subscription after it has
   * initialized, and once B has subscribed.
   * <p>
   * A -----> B's Sink -> B -------> downstream chain
   * <p>
   * In this method, A corresponds to <it>deferredSubscriber</it>; and B to <it>triggeringSubscriber</it>.
   *
   * @param triggeringSubscriber the {@link Flux} whose subscription will trigger the subscription of the
   *        <it>deferredSubscriber</it> {@link Flux}, on the same context as the former one.
   * @param deferredSubscriber the {@link Flux} whose subscription will be deferred
   * @return the triggeringSubscriber {@link Flux}, decorated with the callback that will perform this deferred subscription.
   * @since 4.3
   */
  public static <T> Flux<T> subscribeFluxOnPublisherSubscription(Flux<T> triggeringSubscriber,
                                                                 Flux<T> deferredSubscriber) {
    return triggeringSubscriber
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub.doOnSubscribe(s -> deferredSubscriber.subscriberContext(ctx).subscribe())));
  }

  /**
   * Transform a given {@link Publisher} using a {@link ReactiveProcessor}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param processor the processor to transform publisher with
   * @return the transformed publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> transform(Publisher<CoreEvent> publisher, ReactiveProcessor processor) {
    return Flux.from(publisher).transform(processor);
  }

  /**
   * Transform a given {@link Publisher} using a mapper function. Primarily for use in the implementation of mapping in other
   * class-loaders.
   *
   * @param publisher the publisher to transform
   * @param mapper the mapper to map publisher items with
   * @return the transformed publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> map(Publisher<CoreEvent> publisher, Function<CoreEvent, CoreEvent> mapper) {
    return Flux.from(publisher).map(mapper);
  }

  /**
   * Perform processing using the provided {@link Function} for each {@link CoreEvent}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param function the function to apply to each event.
   * @param component the component that implements this functionality.
   * @return the transformed publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> flatMap(Publisher<CoreEvent> publisher,
                                             Function<CoreEvent, Publisher<CoreEvent>> function, Component component) {
    return Flux.from(publisher)
        .flatMap(event -> from(function.apply(event))
            .onErrorMap(e -> !(e instanceof MessagingException), e -> new MessagingException(event, e, component)));
  }

  /**
   * Creates a new {@link Publisher} that will emit the given {@code event}, publishing it on the given {@code executor}.
   *
   * @param event the {@link CoreEvent} to emit
   * @param executor the thread pool where the event will be published.
   * @return the created publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> justPublishOn(CoreEvent event, ExecutorService executor) {
    return Flux.just(event).publishOn(fromExecutorService(executor));
  }

  /**
   * Creates a {@link Supplier} that on {@link Supplier#get()} invocation will create and subscribe a new {@link Flux} configured
   * through the given {@code configurer}.
   *
   * @param configurer a {@link Function} that receives the blank {@link Flux} and returns a configured one
   * @param <T> the Flux generic type
   * @return a {@link Supplier} that returns a new {@link FluxSink} each time.
   */
  public static <T> Supplier<FluxSink<T>> createFluxSupplier(Function<Flux<T>, Flux<?>> configurer) {
    return () -> {
      final FluxSinkRecorder<T> sinkRef = new FluxSinkRecorder<>();
      Flux<?> flux = configurer.apply(Flux.create(sinkRef));

      flux.subscribe();
      return sinkRef.getFluxSink();
    };
  }

  /**
   * Returns a {@link RoundRobinFluxSinkSupplier} of the given {@code size}. The underlying fluxes are configured through the
   * given {@code configurer}.
   *
   * @param configurer a {@link Function} that receives the blank {@link Flux} and returns a configured one
   * @param size the round robin size
   * @param <T> the Flux generic type
   * @return a new {@link FluxSinkSupplier}
   */
  public static <T> FluxSinkSupplier<T> createRoundRobinFluxSupplier(Function<Flux<T>, Flux<?>> configurer, int size) {
    return new RoundRobinFluxSinkSupplier<>(size, createFluxSupplier(configurer));
  }

  /**
   * Allows to propagate errors downstream from a callback without canceling the downstream publisher, properly executing the
   * error handling operators (i.e.: {@link Flux#onErrorContinue(java.util.function.BiConsumer)}).
   *
   * @param upstream the original publisher to apply the {@code transformer} to.
   * @param transformer the operations to perform on {@code upstream}. A method form the received callback must <b>ALWAYS</b> be
   *        called once.
   * @return a {@link Publisher} where the result provided to the callback in the transformer is pushed.
   *
   * @since 4.3
   */
  public static <T> Publisher<T> withDownstreamErrorPropagation(Publisher<T> upstream,
                                                                BiFunction<Publisher<T>, CompletableCallback<T>, Publisher<T>> transformer) {
    FluxSinkRecorder<Either<Throwable, T>> sinkRecorder = new FluxSinkRecorder<>();

    return subscribeFluxOnPublisherSubscription(create(sinkRecorder)
        .map(result -> {
          result.applyLeft(t -> {
            throw propagate(t);
          });
          return result.getRight();
        }), Flux.from(upstream)
            .transform(p -> transformer.apply(p, new CompletableCallback<T>() {

              @Override
              public void complete(T value) {
                sinkRecorder.next(right(Throwable.class, value));
              }

              @Override
              public void error(Throwable e) {
                // if `sink.error` is called here, it will cancel the flux altogether. That's why an `Either` is used here, so the
                // error can be propagated afterwards in a way consistent with our expected error handling.
                sinkRecorder.next(left(e));
              }

            }))
            .doOnComplete(() -> sinkRecorder.complete()));
  }
}
