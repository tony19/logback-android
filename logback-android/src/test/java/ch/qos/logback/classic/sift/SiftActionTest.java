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
package ch.qos.logback.classic.sift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.sift.AppenderFactory;

public class SiftActionTest {

  LoggerContext context = new LoggerContext();
  InterpretationContext ic = new InterpretationContext(context, null);
  SiftAction action = new SiftAction();
  Attributes attributes = new AttributesImpl();

  SaxEvent siftStart = mock(SaxEvent.class);
  SaxEvent appenderStart = mock(SaxEvent.class);
  SaxEvent appenderEnd = mock(SaxEvent.class);
  SaxEvent siftEnd = mock(SaxEvent.class);

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void beginRegistersTheActionAsInPlayListenerWithAnEmptyEventList() throws ActionException {
    assertNull(action.getSeList());

    action.begin(ic, "sift", attributes);

    assertFalse(ic.isListenerListEmpty());
    assertTrue(action.getSeList().isEmpty());
  }

  @Test
  public void eventsInPlayAreRecordedInOrder() throws ActionException {
    action.begin(ic, "sift", attributes);

    action.inPlay(siftStart);
    action.inPlay(appenderStart);

    assertEquals(Arrays.asList(siftStart, appenderStart), action.getSeList());
  }

  @Test
  public void endHandsTheRecordedEventsToTheSiftingAppenderOnTopOfTheStack() throws ActionException {
    FactoryRecordingSiftingAppender sa = new FactoryRecordingSiftingAppender();
    MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
    discriminator.setKey("userid");
    sa.setDiscriminator(discriminator);
    ic.pushObject(sa);

    action.begin(ic, "sift", attributes);
    for (SaxEvent e : Arrays.asList(siftStart, appenderStart, appenderEnd, siftEnd)) {
      action.inPlay(e);
    }
    action.end(ic, "sift");

    assertTrue(ic.isListenerListEmpty());
    assertSame(sa, ic.peekObject());
    assertTrue(sa.receivedFactory instanceof AppenderFactoryUsingJoran);
    AppenderFactoryUsingJoran factory = (AppenderFactoryUsingJoran) sa.receivedFactory;
    // the enclosing <sift> element is dropped
    assertEquals(Arrays.asList(appenderStart, appenderEnd), factory.getEventList());
    // the discriminator key is handed over
    assertEquals(SiftingJoranConfigurator.class.getName() + "{userid=alice}",
        factory.getSiftingJoranConfigurator("alice").toString());
  }

  @Test
  public void endIgnoresAnObjectOtherThanASiftingAppenderOnTopOfTheStack() throws ActionException {
    Object notASiftingAppender = new Object();
    ic.pushObject(notASiftingAppender);

    action.begin(ic, "sift", attributes);
    action.inPlay(siftStart);
    action.end(ic, "sift");

    assertTrue(ic.isListenerListEmpty());
    assertSame(notASiftingAppender, ic.peekObject());
    assertEquals(Arrays.asList(siftStart), action.getSeList());
  }

  static class FactoryRecordingSiftingAppender extends SiftingAppender {
    AppenderFactory<ILoggingEvent> receivedFactory;

    @Override
    public void setAppenderFactory(AppenderFactory<ILoggingEvent> appenderFactory) {
      this.receivedFactory = appenderFactory;
      super.setAppenderFactory(appenderFactory);
    }
  }
}
