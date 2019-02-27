/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.classic.net;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * An abstract base for components that receive logging events from a remote
 * peer and log according to local policy
 *
 * @author Carl Harris
 */
public abstract class ReceiverBase extends ContextAwareBase
    implements LifeCycle {

  private boolean started;

  /**
   * {@inheritDoc}
   */
  public final void start() {
    if (isStarted()) return;
    if (getContext() == null) {
      throw new IllegalStateException("context not set");
    }
    if (shouldStart()) {
      getContext().getScheduledExecutorService().execute(getRunnableTask());
      started = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  public final void stop() {
    if (!isStarted()) return;
    try {
      onStop();
    }
    catch (RuntimeException ex) {
      addError("on stop: " + ex, ex);
    }
    started = false;
  }

  /**
   * {@inheritDoc}
   */
  public final boolean isStarted() {
    return started;
  }

  /**
   * Determines whether this receiver should start.
   * <p>
   * Subclasses will implement this method to do any subclass-specific
   * validation.  The subclass's {@link #getRunnableTask()} method will be
   * invoked (and the task returned will be submitted to the executor)
   * if and only if this method returns {@code true}
   * @return flag indicating whether this receiver should start
   */
  protected abstract boolean shouldStart();

  /**
   * Allows a subclass to participate in receiver shutdown.
   */
  protected abstract void onStop();

  /**
   * Provides the runnable task this receiver will execute.
   * @return runnable task
   */
  protected abstract Runnable getRunnableTask();

}
