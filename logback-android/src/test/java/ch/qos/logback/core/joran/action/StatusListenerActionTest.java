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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.DynamicClassLoadingException;

/**
 * Tests {@link StatusListenerAction}.
 */
public class StatusListenerActionTest {

  private final Context context = new ContextBase();
  private final StatusManager statusManager = context.getStatusManager();
  private final Interpreter interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
  private final InterpretationContext ic = interpreter.getInterpretationContext();
  private final StatusListenerAction action = new StatusListenerAction();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void contextAwareLifeCycleListenerIsAddedConfiguredAndStarted() throws ActionException {
    action.begin(ic, "statusListener", listenerAttributes(StartableStatusListenerActionListener.class.getName()));

    StartableStatusListenerActionListener listener = (StartableStatusListenerActionListener) ic.peekObject();
    assertTrue(statusManager.getCopyOfStatusListenerList().contains(listener));
    assertSame(context, listener.getContext());
    assertFalse(listener.isStarted());
    assertStatus(Status.INFO,
        "Added status listener of type [" + StartableStatusListenerActionListener.class.getName() + "]");

    action.end(ic, "statusListener");

    assertTrue(listener.isStarted());
    assertTrue(ic.isEmpty());
  }

  @Test
  public void plainListenerIsAddedAndPopped() throws ActionException {
    action.begin(ic, "statusListener", listenerAttributes(PlainStatusListenerActionListener.class.getName()));

    Object listener = ic.peekObject();
    assertTrue(listener instanceof PlainStatusListenerActionListener);
    assertTrue(statusManager.getCopyOfStatusListenerList().contains(listener));

    action.end(ic, "statusListener");

    assertTrue(ic.isEmpty());
    assertTrue(statusManager.getCopyOfStatusListenerList().contains(listener));
  }

  @Test
  public void listenerRejectedByTheStatusManagerIsNotStarted() throws ActionException {
    OnConsoleStatusListener present = new OnConsoleStatusListener();
    statusManager.add(present);

    action.begin(ic, "statusListener", listenerAttributes(OnConsoleStatusListener.class.getName()));
    OnConsoleStatusListener rejected = (OnConsoleStatusListener) ic.peekObject();
    action.end(ic, "statusListener");

    assertFalse(rejected.isStarted());
    assertTrue(ic.isEmpty());
    List<StatusListener> listeners = statusManager.getCopyOfStatusListenerList();
    assertEquals(1, listeners.size());
    assertSame(present, listeners.get(0));
  }

  @Test
  public void listenerNotKnownToBeAddedIsNotStarted() {
    // end() without a begin() that added the listener
    StartableStatusListenerActionListener listener = new StartableStatusListenerActionListener();
    action.statusListener = listener;
    ic.pushObject(listener);

    action.end(ic, "statusListener");

    assertFalse(listener.isStarted());
    assertTrue(ic.isEmpty());
  }

  @Test
  public void missingClassNameIsReportedWithTheLineNumber() throws ActionException {
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(12);
    interpreter.setDocumentLocator(locator);

    action.begin(ic, "statusListener", listenerAttributes(""));

    assertTrue(action.inError);
    assertTrue(ic.isEmpty());
    assertTrue(statusManager.getCopyOfStatusListenerList().isEmpty());
    assertStatus(Status.ERROR, "Missing class name for statusListener. Near [statusListener] line 12");
    int statusCount = statusManager.getCount();

    ic.pushObject("unrelated");
    action.end(ic, "statusListener");

    assertSame("unrelated", ic.peekObject());
    assertEquals(statusCount, statusManager.getCount());
  }

  @Test
  public void uninstantiableListenerIsReportedAndRethrown() {
    ActionException e = assertThrows(ActionException.class,
        () -> action.begin(ic, "statusListener", listenerAttributes("no.such.Listener")));

    assertTrue(e.getCause() instanceof DynamicClassLoadingException);
    assertTrue(action.inError);
    assertTrue(ic.isEmpty());
    Status error = assertStatus(Status.ERROR, "Could not create an StatusListener of type [no.such.Listener].");
    assertSame(e.getCause(), error.getThrowable());
  }

  @Test
  public void foreignObjectOnTopOfTheStackIsReported() throws ActionException {
    action.begin(ic, "statusListener", listenerAttributes(PlainStatusListenerActionListener.class.getName()));
    Object foreign = new Object();
    ic.pushObject(foreign);

    action.end(ic, "statusListener");

    assertSame(foreign, ic.peekObject());
    assertEquals(2, ic.getObjectStack().size());
    assertStatus(Status.WARN, "The object at the of the stack is not the statusListener pushed earlier.");
  }

  @Test
  public void finishDoesNothing() {
    ic.pushObject("top");

    action.finish(ic);

    assertSame("top", ic.peekObject());
    assertEquals(0, statusManager.getCount());
  }

  private static DummyAttributes listenerAttributes(String className) {
    DummyAttributes atts = new DummyAttributes();
    atts.setValue(Action.CLASS_ATTRIBUTE, className);
    return atts;
  }

  private Status assertStatus(int level, String message) {
    for (Status s : statusManager.getCopyOfStatusList()) {
      if (message.equals(s.getMessage())) {
        assertEquals(level, s.getLevel());
        return s;
      }
    }
    throw new AssertionError("no status [" + message + "] in " + statusManager.getCopyOfStatusList());
  }

  /** A status listener that is neither context-aware nor startable. */
  public static class PlainStatusListenerActionListener implements StatusListener {
    public void addStatusEvent(Status status) {
    }
  }

  /** A context-aware, startable status listener. */
  public static class StartableStatusListenerActionListener extends ContextAwareBase
      implements StatusListener, LifeCycle {
    private boolean started;

    public void addStatusEvent(Status status) {
    }

    public void start() {
      started = true;
    }

    public void stop() {
      started = false;
    }

    public boolean isStarted() {
      return started;
    }
  }
}
