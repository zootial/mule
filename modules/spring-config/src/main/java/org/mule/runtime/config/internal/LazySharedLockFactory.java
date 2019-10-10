package org.mule.runtime.config.internal;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lock.LockFactory;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;


/**
 * LockFactory implementation that is to be used across apps.
 */
public class LazySharedLockFactory implements LockFactory, Initialisable {

  private LockFactory lockFactory;
  private Supplier<LockFactory> lockFactorySupplier;

  public LazySharedLockFactory(Supplier<LockFactory> lockFactorySupplier) {
    this.lockFactorySupplier = lockFactorySupplier;
  }

  @Override
  public Lock createLock(String lockId) {
    return lockFactory.createLock(lockId);
  }

  @Override
  public void initialise() throws InitialisationException {
    this.lockFactory = lockFactorySupplier.get();
  }
}
