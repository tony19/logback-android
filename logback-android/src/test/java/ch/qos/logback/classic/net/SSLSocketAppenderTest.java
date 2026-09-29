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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 * Unit tests for {@link SSLSocketAppender}.
 */
public class SSLSocketAppenderTest {

  private final SSLSocketAppender appender = new SSLSocketAppender();

  @Test
  public void callerDataIsNotComputedByDefault() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    appender.postProcessEvent(event);
    verify(event, never()).getCallerData();
  }

  @Test
  public void callerDataIsComputedWhenIncluded() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    appender.setIncludeCallerData(true);
    appender.postProcessEvent(event);
    verify(event).getCallerData();
  }

  @Test
  public void callerDataIsNotComputedWhenExcludedAgain() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    appender.setIncludeCallerData(true);
    appender.setIncludeCallerData(false);
    appender.postProcessEvent(event);
    verify(event, never()).getCallerData();
  }

  @Test
  public void transformerSerializesLoggingEventsAsValueObjects() {
    PreSerializationTransformer<ILoggingEvent> pst = appender.getPST();
    assertTrue(pst instanceof LoggingEventPreSerializationTransformer);
    assertSame(pst, appender.getPST());

    LoggingEvent event = new LoggingEvent(getClass().getName(),
        new LoggerContext().getLogger("ssl"), Level.INFO, "msg", null, null);
    assertTrue(pst.transform(event) instanceof LoggingEventVO);
  }
}
