/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * javadoc needed
 */
class WhileRouter {

  private final Logger LOGGER = getLogger(WhileRouter.class);

  private final Component owner;
  private final Predicate<CoreEvent> iterateAgain;

  private final Flux<CoreEvent> upstreamFlux;
  private final Flux<CoreEvent> innerFlux;
  private final Flux<CoreEvent> downstreamFlux;


  private final FluxSinkRecorder<CoreEvent> innerRecorder = new FluxSinkRecorder<>();
  private final FluxSinkRecorder<Either<Throwable, CoreEvent>> downstreamRecorder = new FluxSinkRecorder<>();

  // When using an while scope in a blocking flow (for example, calling the owner flow with a Processor#process call),
  // this leads to a reactor completion signal being emitted while the event is being re-injected for retrials. This is solved by
  // deferring the downstream publisher completion until all events have evacuated the scope.
  private final AtomicInteger inflightEvents = new AtomicInteger(0);
  private final AtomicBoolean completeDeferred = new AtomicBoolean(false);

  WhileRouter(Component owner, Publisher<CoreEvent> publisher, MessageProcessorChain nestedChain,
              Predicate<CoreEvent> iterateAgain) {
    this.owner = owner;
    this.iterateAgain = iterateAgain;


    upstreamFlux = Flux.from(publisher)
        .doOnNext(event -> {
          // Inject event into retrial execution chain
          inflightEvents.getAndIncrement();
          if (iterateAgain.test(event)) {
            innerRecorder.next(event);
          } else {
            downstreamRecorder.next(right(Throwable.class, event));
            completeRouterIfNecessary();
          }

        })
        .doOnComplete(() -> {
          if (inflightEvents.get() == 0) {
            completeRouter();
          } else {
            completeDeferred.set(true);
          }
        });

    // Inner chain. Contains all retrial and error handling logic.
    innerFlux = Flux.create(innerRecorder)
        // Assume: resolver.currentContextForEvent(publishedEvent) is current context
        .transform(innerPublisher -> applyWithChildContext(innerPublisher, nestedChain,
                                                           Optional.of(owner.getLocation())))
        .doOnNext(successfulEvent -> {
          // Scope execution was successful, pop current ctx
          if (iterateAgain.test(successfulEvent)) {
            innerRecorder.next(successfulEvent);
          } else {
            downstreamRecorder.next(right(Throwable.class, successfulEvent));
            completeRouterIfNecessary();
          }
        }).onErrorContinue(MessagingException.class, (error, offendingEvent) -> {
          downstreamRecorder.next(left(error, CoreEvent.class));
          completeRouterIfNecessary();
        });

    // Downstream chain. Unpacks and publishes successful events and errors downstream.
    downstreamFlux = Flux.create(downstreamRecorder)
        .doOnNext(event -> inflightEvents.decrementAndGet())
        .map(getScopeResultMapper());

  }

  private Function<Either<Throwable, CoreEvent>, CoreEvent> getScopeResultMapper() {
    return either -> {
      if (either.isLeft()) {
        throw propagate(either.getLeft());
      } else {
        return either.getRight();
      }
    };
  }

  private void completeRouterIfNecessary() {
    if (completeDeferred.get() && inflightEvents.get() == 0) {
      completeRouter();
    }
  }

  /**
   * Complete both downstream publishers.
   */
  private void completeRouter() {
    innerRecorder.complete();
    downstreamRecorder.complete();
  }


  /**
   * Assembles and returns the downstream {@link Publisher<CoreEvent>}.
   *
   * @return the successful {@link CoreEvent} or retries exhaustion errors {@link Publisher}
   */
  Publisher<CoreEvent> getDownstreamPublisher() {
    return downstreamFlux
        .compose(downstreamPublisher -> subscriberContext()
            .flatMapMany(downstreamContext -> downstreamPublisher.doOnSubscribe(s -> {
              innerFlux.subscriberContext(downstreamContext).subscribe();
              upstreamFlux.subscriberContext(downstreamContext).subscribe();
            })));
  }

}
