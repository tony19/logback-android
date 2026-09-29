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
import static org.junit.Assert.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;

public class EventPlayerTest {

  @Test
  public void copyOfPlayerEventListIsADetachedCopyOfThePlayedEvents() {
    Context context = new ContextBase();
    SimpleRuleStore ruleStore = new SimpleRuleStore(context);
    ruleStore.addRule(new ElementSelector("x"), new NOPAction());
    Interpreter interpreter = new Interpreter(context, ruleStore, new ElementPath());

    SaxEventRecorder recorder = new SaxEventRecorder(context);
    recorder.setDocumentLocator(new LocatorImpl());
    recorder.startElement("", "x", "x", new AttributesImpl());
    recorder.endElement("", "x", "x");
    List<SaxEvent> events = new ArrayList<SaxEvent>(recorder.getSaxEventList());

    EventPlayer player = interpreter.getEventPlayer();
    player.play(events);

    List<SaxEvent> copy = player.getCopyOfPlayerEventList();
    assertEquals(events, copy);
    assertNotSame(events, copy);

    copy.clear();
    assertEquals(2, player.getCopyOfPlayerEventList().size());
    assertEquals(0, context.getStatusManager().getCount());
  }
}
