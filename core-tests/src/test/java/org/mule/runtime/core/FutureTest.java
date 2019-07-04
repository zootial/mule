/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class FutureTest {

  ExecutorService cpuIntensiveScheduler = Executors.newFixedThreadPool(5);

  @Test
  public void syncExample() {
    Runnable task = () -> sync();
    task.run();
  }

  @Test
  public void asyncExample() throws Exception {
    new Product().getPriceAsync()
        .thenComposeAsync(this::applyTax)
        .thenAccept(System.out::println)
        .whenCompleteAsync((v, e) -> {
          if (e != null) {
            System.out.println("CALL MILEI!!!!!");
          }
        })
    .get();
  }

  
  public void sync() {
    Product product = new Product();

    double price = product.getPrice();
    //double afterTax = applyTax(price);
    //
    //System.out.println(afterTax);
  }

  public CompletableFuture<Double> applyTax(double price) {
    CompletableFuture<Double> future = new CompletableFuture<>();
    cpuIntensiveScheduler.submit(() -> {
      future.complete(calculateTax(price));
    });

    return future;
  }

  private double calculateTax(double price) {
    double afterTax = price * 1.21;
    if (afterTax > price) {
      afterTax = evadeTax(afterTax);
    }
    return afterTax;
  }


  public double evadeTax(double amount) {
    throw new AFIPException();
  }


  //  return afterTax;
  //}

  private class Product {

    public double getPrice() {
      return 21.5;
    }

    public CompletableFuture<Double> getPriceAsync() {
      return CompletableFuture.completedFuture(getPrice());
    }
  }

  public class AFIPException extends RuntimeException {

  }

}
