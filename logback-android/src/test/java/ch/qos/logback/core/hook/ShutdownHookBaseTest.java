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
package ch.qos.logback.core.hook;

import org.junit.Test;

import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.status.StatusManager;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ShutdownHookBaseTest {

  /** A hook that stops the context as soon as it runs. */
  static class ImmediateShutdownHook extends ShutdownHookBase {
    @Override
    public void run() {
      stop();
    }
  }

  private final ImmediateShutdownHook hook = new ImmediateShutdownHook();

  @Test
  public void stopStopsAContextBase() {
    ContextBase context = new ContextBase();
    context.start();
    hook.setContext(context);

    hook.run();

    assertFalse(context.isStarted());
    new StatusChecker(context).assertContainsMatch(Status.INFO, "Logback context being closed via shutdown hook");
  }

  @Test
  public void stopOnlyReportsForAContextThatIsNotAContextBase() {
    StatusManager statusManager = new BasicStatusManager();
    Context context = mock(Context.class, withSettings().extraInterfaces(LifeCycle.class));
    when(context.getStatusManager()).thenReturn(statusManager);
    hook.setContext(context);

    hook.run();

    new StatusChecker(statusManager).assertContainsMatch(Status.INFO, "Logback context being closed via shutdown hook");
    verify((LifeCycle) context, never()).stop();
  }
}
