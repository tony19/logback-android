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
package ch.qos.logback.core.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.hook.DefaultShutdownHook;
import ch.qos.logback.core.hook.ShutdownHookBase;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.DynamicClassLoadingException;

/**
 * Tests {@link ShutdownHookAction}.
 */
public class ShutdownHookActionTest {

  private final Context context = new ContextBase();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final ShutdownHookAction action = new ShutdownHookAction();

  @Before
  public void setUp() {
    context.setName("hookTest");
    action.setContext(context);
  }

  @After
  public void tearDown() {
    // never leave a hook registered with the JVM
    Object hookThread = context.getObject(CoreConstants.SHUTDOWN_HOOK_THREAD);
    if (hookThread != null) {
      Runtime.getRuntime().removeShutdownHook((Thread) hookThread);
    }
  }

  @Test
  public void declaredHookIsInstantiatedAndRegisteredWithTheJvm() throws ActionException {
    action.begin(ic, "shutdownHook", hookAttributes(ShutdownHookActionHook.class.getName()));

    ShutdownHookActionHook hook = (ShutdownHookActionHook) ic.peekObject();
    assertSame(hook, action.hook);
    assertSame(context, hook.getContext());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals("About to instantiate shutdown hook of type [" + ShutdownHookActionHook.class.getName() + "]",
        statuses.get(0).getMessage());

    action.end(ic, "shutdownHook");

    assertTrue(ic.isEmpty());
    Thread hookThread = (Thread) context.getObject(CoreConstants.SHUTDOWN_HOOK_THREAD);
    assertEquals("Logback shutdown hook [hookTest]", hookThread.getName());
    // removal succeeds only for a registered hook
    assertTrue(Runtime.getRuntime().removeShutdownHook(hookThread));
  }

  @Test
  public void defaultHookIsAssumedWithoutClassName() throws ActionException {
    action.begin(ic, "shutdownHook", hookAttributes(""));

    assertTrue(ic.peekObject() instanceof DefaultShutdownHook);
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals("Assuming className [" + DefaultShutdownHook.class.getName() + "]",
        statuses.get(0).getMessage());
  }

  @Test
  public void uninstantiableHookIsReportedAndNothingIsRegistered() throws ActionException {
    ActionException e = assertThrows(ActionException.class,
        () -> action.begin(ic, "shutdownHook", hookAttributes("no.such.Hook")));

    assertTrue(e.getCause() instanceof DynamicClassLoadingException);
    assertTrue(ic.isEmpty());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    Status error = statuses.get(statuses.size() - 1);
    assertEquals(Status.ERROR, error.getLevel());
    assertEquals("Could not create a shutdown hook of type [no.such.Hook].", error.getMessage());
    assertSame(e.getCause(), error.getThrowable());
    int statusCount = statuses.size();

    ic.pushObject("unrelated");
    action.end(ic, "shutdownHook");

    assertSame("unrelated", ic.peekObject());
    assertNull(context.getObject(CoreConstants.SHUTDOWN_HOOK_THREAD));
    assertEquals(statusCount, context.getStatusManager().getCount());
  }

  @Test
  public void foreignObjectOnTopOfTheStackIsReportedAndNothingIsRegistered() throws ActionException {
    action.begin(ic, "shutdownHook", hookAttributes(ShutdownHookActionHook.class.getName()));
    Object foreign = new Object();
    ic.pushObject(foreign);

    action.end(ic, "shutdownHook");

    assertSame(foreign, ic.peekObject());
    assertEquals(2, ic.getObjectStack().size());
    assertNull(context.getObject(CoreConstants.SHUTDOWN_HOOK_THREAD));
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    Status warning = statuses.get(statuses.size() - 1);
    assertEquals(Status.WARN, warning.getLevel());
    assertEquals("The object at the of the stack is not the hook pushed earlier.", warning.getMessage());
  }

  private static DummyAttributes hookAttributes(String className) {
    DummyAttributes atts = new DummyAttributes();
    atts.setValue(Action.CLASS_ATTRIBUTE, className);
    return atts;
  }

  /** A shutdown hook that does nothing when run. */
  public static class ShutdownHookActionHook extends ShutdownHookBase {
    public void run() {
    }
  }
}
