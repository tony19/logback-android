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
package ch.qos.logback.core.joran.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.event.BodyEvent;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.event.StartEvent;
import ch.qos.logback.core.status.Status;

public class InterpreterTest {

  private final Context context = new ContextBase();
  private final SimpleRuleStore ruleStore = new SimpleRuleStore(context);
  private final Interpreter interpreter = new Interpreter(context, ruleStore, new ElementPath());
  private final List<String> calls = new ArrayList<String>();

  /** Records the calls it receives; optionally fails in begin() or body(). */
  class RecordingAction extends Action {
    final String id;
    RuntimeException beginFailure;
    ActionException bodyFailure;

    RecordingAction(String id) {
      this.id = id;
    }

    @Override
    public void begin(InterpretationContext ic, String name, Attributes attributes) {
      calls.add(id + ".begin " + name);
      if (beginFailure != null) {
        throw beginFailure;
      }
    }

    @Override
    public void body(InterpretationContext ic, String body) throws ActionException {
      calls.add(id + ".body " + body);
      if (bodyFailure != null) {
        throw bodyFailure;
      }
    }

    @Override
    public void end(InterpretationContext ic, String name) {
      calls.add(id + ".end " + name);
    }

    @Override
    public String toString() {
      return "RecordingAction(" + id + ")";
    }
  }

  private static SaxEventRecorder recorder() {
    SaxEventRecorder recorder = new SaxEventRecorder();
    recorder.setDocumentLocator(new LocatorImpl());
    return recorder;
  }

  private static StartEvent startEvent(String name) {
    SaxEventRecorder recorder = recorder();
    recorder.startElement("", name, name, new AttributesImpl());
    return (StartEvent) recorder.getSaxEventList().get(0);
  }

  private static BodyEvent bodyEvent(String text) {
    SaxEventRecorder recorder = recorder();
    recorder.characters(text.toCharArray(), 0, text.length());
    return (BodyEvent) recorder.getSaxEventList().get(0);
  }

  private static EndEvent endEvent(String name) {
    SaxEventRecorder recorder = recorder();
    recorder.endElement("", name, name);
    return (EndEvent) recorder.getSaxEventList().get(0);
  }

  private static LocatorImpl locatorAt(int line, int column) {
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(line);
    locator.setColumnNumber(column);
    return locator;
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  @Test
  public void accessorsExposeTheCollaborators() {
    assertSame(ruleStore, interpreter.getRuleStore());
    assertSame(interpreter.getInterpretationContext(), interpreter.getExecutionContext());
    assertSame(interpreter, interpreter.getInterpretationContext().getJoranInterpreter());
    assertNotNull(interpreter.getEventPlayer());
  }

  @Test
  public void startDocumentChangesNothing() {
    interpreter.startDocument();

    assertNull(interpreter.getLocator());
    assertTrue(interpreter.actionListStack.isEmpty());
    assertTrue(interpreter.getInterpretationContext().isEmpty());
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void tagNameFallsBackToQNameWhenLocalNameIsNullOrEmpty() {
    assertEquals("local", interpreter.getTagName("local", "p:q"));
    assertEquals("p:q", interpreter.getTagName(null, "p:q"));
    assertEquals("p:q", interpreter.getTagName("", "p:q"));
  }

  @Test
  public void missingOrBlankBodyTextIsNotPassedToActions() {
    ruleStore.addRule(new ElementSelector("x"), new RecordingAction("a"));
    interpreter.startElement(startEvent("x"));

    BodyEvent withoutText = mock(BodyEvent.class);
    when(withoutText.getText()).thenReturn(null);
    BodyEvent blank = mock(BodyEvent.class);
    when(blank.getText()).thenReturn("  ");
    interpreter.characters(withoutText);
    interpreter.characters(blank);
    interpreter.characters(bodyEvent(" text "));

    assertEquals(Arrays.asList("a.begin x", "a.body text"), calls);
  }

  @Test
  public void failingBodyActionIsReportedAndTheNextActionStillRuns() {
    RecordingAction failing = new RecordingAction("a");
    ActionException failure = new ActionException(new IllegalArgumentException("bad body"));
    failing.bodyFailure = failure;
    ruleStore.addRule(new ElementSelector("x"), failing);
    ruleStore.addRule(new ElementSelector("x"), new RecordingAction("b"));

    interpreter.startElement(startEvent("x"));
    interpreter.characters(bodyEvent("text"));
    interpreter.endElement(endEvent("x"));

    assertEquals(Arrays.asList("a.begin x", "b.begin x", "a.body text", "b.body text", "a.end x", "b.end x"),
        calls);
    List<Status> statuses = statuses();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Exception in end() methd for action [RecordingAction(a)]", statuses.get(0).getMessage());
    assertSame(failure, statuses.get(0).getThrowable());
  }

  @Test
  public void nullActionListsAreIgnored() {
    interpreter.callBeginAction(null, "x", new AttributesImpl());

    interpreter.actionListStack.push(null);
    interpreter.characters(bodyEvent("text"));
    interpreter.endElement(endEvent("x"));

    assertTrue(interpreter.actionListStack.isEmpty());
    assertNull(interpreter.skip);
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void errorOriginCarriesLocatorPositionWhenKnown() {
    RecordingAction failing = new RecordingAction("a");
    failing.beginFailure = new IllegalStateException("bad begin");
    List<Action> actions = Collections.<Action>singletonList(failing);

    interpreter.callBeginAction(actions, "x", new AttributesImpl());
    interpreter.setDocumentLocator(locatorAt(3, 5));
    interpreter.callBeginAction(actions, "x", new AttributesImpl());

    List<Status> statuses = statuses();
    assertEquals(2, statuses.size());
    assertEquals("RuntimeException in Action for tag [x]", statuses.get(0).getMessage());
    assertEquals(Interpreter.class.getName() + "@NA:NA", statuses.get(0).getOrigin());
    assertEquals(Interpreter.class.getName() + "@3:5", statuses.get(1).getOrigin());
  }
}
