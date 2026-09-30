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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.ReceiverBase;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.IncompatibleClassException;

public class ReceiverActionTest {

  private final LoggerContext context = new LoggerContext();
  private final Interpreter interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
  private final InterpretationContext ic = interpreter.getInterpretationContext();
  private final ReceiverAction action = new ReceiverAction();

  @Before
  public void setUp() {
    action.setContext(context);
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(7);
    interpreter.setDocumentLocator(locator);
  }

  @After
  public void tearDown() {
    context.stop();
  }

  @Test
  public void missingClassNameIsReportedWithTheLine() throws Exception {
    action.begin(ic, "receiver", new AttributesImpl());

    assertTrue(ic.isEmpty());
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Missing class name for receiver. Near [receiver] line 7", statuses.get(0).getMessage());
  }

  @Test
  public void endAfterMissingClassNameDoesNothing() throws Exception {
    Object parent = new Object();
    ic.pushObject(parent);
    action.begin(ic, "receiver", new AttributesImpl());

    action.end(ic, "receiver");

    assertSame(parent, ic.peekObject());
    assertEquals(1, statuses().size());
  }

  @Test
  public void receiverIsInstantiatedGivenTheContextAndPushed() throws Exception {
    action.begin(ic, "receiver", classAttribute(ReceiverActionTestReceiver.class.getName()));

    ReceiverActionTestReceiver receiver = (ReceiverActionTestReceiver) ic.peekObject();
    assertSame(context, receiver.getContext());
    assertFalse(receiver.isStarted());
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.INFO, statuses.get(0).getLevel());
    assertEquals("About to instantiate receiver of type [" + ReceiverActionTestReceiver.class.getName() + "]",
        statuses.get(0).getMessage());
  }

  @Test
  public void endStartsTheReceiverRegistersItWithTheContextAndPopsIt() throws Exception {
    action.begin(ic, "receiver", classAttribute(ReceiverActionTestReceiver.class.getName()));
    ReceiverActionTestReceiver receiver = (ReceiverActionTestReceiver) ic.peekObject();

    action.end(ic, "receiver");

    assertTrue(receiver.isStarted());
    assertTrue(ic.isEmpty());
    // registered components are stopped when the context is reset
    context.reset();
    assertFalse(receiver.isStarted());
    assertEquals(1, receiver.stopCount);
  }

  @Test
  public void uninstantiableClassIsAnErrorAndAborts() {
    ActionException e = assertThrows(ActionException.class,
        () -> action.begin(ic, "receiver", classAttribute(String.class.getName())));

    assertTrue(e.getCause() instanceof IncompatibleClassException);
    assertTrue(ic.isEmpty());
    List<Status> statuses = statuses();
    Status error = statuses.get(statuses.size() - 1);
    assertEquals(Status.ERROR, error.getLevel());
    assertEquals("Could not create a receiver of type [java.lang.String].", error.getMessage());
    assertSame(e.getCause(), error.getThrowable());
  }

  @Test
  public void endAfterFailedInstantiationDoesNothing() throws Exception {
    Object parent = new Object();
    ic.pushObject(parent);
    assertThrows(ActionException.class,
        () -> action.begin(ic, "receiver", classAttribute(String.class.getName())));
    int statusCount = statuses().size();

    action.end(ic, "receiver");

    assertSame(parent, ic.peekObject());
    assertEquals(statusCount, statuses().size());
  }

  @Test
  public void endWarnsWhenTheReceiverIsNotOnTopOfTheStack() throws Exception {
    action.begin(ic, "receiver", classAttribute(ReceiverActionTestReceiver.class.getName()));
    ReceiverActionTestReceiver receiver = (ReceiverActionTestReceiver) ic.peekObject();
    ic.pushObject("intruder");

    action.end(ic, "receiver");

    // the receiver is still started; only the stack is left as it was
    assertTrue(receiver.isStarted());
    assertEquals("intruder", ic.peekObject());
    assertEquals(2, ic.getObjectStack().size());
    Status warning = statuses().get(statuses().size() - 1);
    assertEquals(Status.WARN, warning.getLevel());
    assertEquals("The object at the of the stack is not the remote pushed earlier.", warning.getMessage());
  }

  private static AttributesImpl classAttribute(String className) {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", Action.CLASS_ATTRIBUTE, Action.CLASS_ATTRIBUTE, "CDATA", className);
    return attributes;
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  /** A receiver that runs a no-op task and counts its stops. */
  public static class ReceiverActionTestReceiver extends ReceiverBase {
    int stopCount;

    @Override
    protected boolean shouldStart() {
      return true;
    }

    @Override
    protected void onStop() {
      stopCount++;
    }

    @Override
    protected Runnable getRunnableTask() {
      return new Runnable() {
        @Override
        public void run() {
        }
      };
    }
  }
}
