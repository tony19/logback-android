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
package ch.qos.logback.classic.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.IncompatibleClassException;

public class LoggerContextListenerActionTest {

  private final LoggerContext context = new LoggerContext();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final LoggerContextListenerAction action = new LoggerContextListenerAction();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void missingClassAttributeIsAnError() throws Exception {
    action.begin(ic, "loggerContextListener", new AttributesImpl());

    assertTrue(action.inError);
    assertTrue(ic.isEmpty());
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Mandatory \"class\" attribute not set for <loggerContextListener> element",
        statuses.get(0).getMessage());
  }

  @Test
  public void endAfterAnErrorRegistersNothing() throws Exception {
    Object parent = new Object();
    ic.pushObject(parent);
    action.begin(ic, "loggerContextListener", new AttributesImpl());

    action.end(ic, "loggerContextListener");

    assertSame(parent, ic.peekObject());
    assertTrue(context.getCopyOfListenerList().isEmpty());
  }

  @Test
  public void classThatIsNotAListenerIsAnError() throws Exception {
    action.begin(ic, "loggerContextListener", classAttribute(String.class.getName()));

    assertTrue(action.inError);
    assertTrue(ic.isEmpty());
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Could not create LoggerContextListener of type java.lang.String].", statuses.get(0).getMessage());
    assertTrue(statuses.get(0).getThrowable() instanceof IncompatibleClassException);

    action.end(ic, "loggerContextListener");
    assertTrue(context.getCopyOfListenerList().isEmpty());
  }

  @Test
  public void contextAwareLifeCycleListenerIsGivenTheContextStartedAndRegistered() throws Exception {
    action.begin(ic, "loggerContextListener", classAttribute(StartableContextListener.class.getName()));

    assertFalse(action.inError);
    StartableContextListener listener = (StartableContextListener) ic.peekObject();
    assertSame(context, listener.getContext());
    assertFalse(listener.isStarted());
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.INFO, statuses.get(0).getLevel());
    assertEquals("Adding LoggerContextListener of type [" + StartableContextListener.class.getName()
        + "] to the object stack", statuses.get(0).getMessage());

    action.end(ic, "loggerContextListener");

    assertTrue(listener.isStarted());
    assertEquals(Collections.<LoggerContextListener>singletonList(listener), context.getCopyOfListenerList());
    assertTrue(ic.isEmpty());
    statuses = statuses();
    assertEquals(statuses.toString(), 2, statuses.size());
    assertEquals(Status.INFO, statuses.get(1).getLevel());
    assertEquals("Starting LoggerContextListener", statuses.get(1).getMessage());
  }

  @Test
  public void plainListenerIsRegisteredWithoutBeingStarted() throws Exception {
    action.begin(ic, "loggerContextListener", classAttribute(PlainContextListener.class.getName()));
    PlainContextListener listener = (PlainContextListener) ic.peekObject();

    action.end(ic, "loggerContextListener");

    assertEquals(Collections.<LoggerContextListener>singletonList(listener), context.getCopyOfListenerList());
    assertTrue(ic.isEmpty());
    assertEquals(0, statusCount("Starting LoggerContextListener"));
  }

  @Test
  public void endWarnsAndRegistersNothingWhenTheListenerIsNotOnTopOfTheStack() throws Exception {
    action.begin(ic, "loggerContextListener", classAttribute(StartableContextListener.class.getName()));
    StartableContextListener listener = (StartableContextListener) ic.peekObject();
    ic.pushObject("intruder");

    action.end(ic, "loggerContextListener");

    assertEquals("intruder", ic.peekObject());
    assertEquals(2, ic.getObjectStack().size());
    assertFalse(listener.isStarted());
    assertTrue(context.getCopyOfListenerList().isEmpty());
    Status warning = statuses().get(statuses().size() - 1);
    assertEquals(Status.WARN, warning.getLevel());
    assertEquals("The object on the top the of the stack is not the LoggerContextListener pushed earlier.",
        warning.getMessage());
  }

  @Test
  public void beginClearsAPreviousError() throws Exception {
    action.begin(ic, "loggerContextListener", new AttributesImpl());
    assertTrue(action.inError);

    action.begin(ic, "loggerContextListener", classAttribute(PlainContextListener.class.getName()));

    assertFalse(action.inError);
    assertNotNull(ic.peekObject());
  }

  private static AttributesImpl classAttribute(String className) {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", Action.CLASS_ATTRIBUTE, Action.CLASS_ATTRIBUTE, "CDATA", className);
    return attributes;
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  private int statusCount(String message) {
    int count = 0;
    for (Status status : statuses()) {
      if (status.getMessage().equals(message)) {
        count++;
      }
    }
    return count;
  }

  /** A listener that is neither {@code ContextAware} nor a {@link LifeCycle}. */
  public static class PlainContextListener implements LoggerContextListener {
    @Override
    public boolean isResetResistant() {
      return false;
    }

    @Override
    public void onStart(LoggerContext context) {
    }

    @Override
    public void onReset(LoggerContext context) {
    }

    @Override
    public void onStop(LoggerContext context) {
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
    }
  }

  /** A context-aware listener with a life cycle. */
  public static class StartableContextListener extends ContextAwareBase
      implements LoggerContextListener, LifeCycle {
    private boolean started;

    @Override
    public void start() {
      started = true;
    }

    @Override
    public void stop() {
      started = false;
    }

    @Override
    public boolean isStarted() {
      return started;
    }

    @Override
    public boolean isResetResistant() {
      return false;
    }

    @Override
    public void onStart(LoggerContext context) {
    }

    @Override
    public void onReset(LoggerContext context) {
    }

    @Override
    public void onStop(LoggerContext context) {
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
    }
  }
}
