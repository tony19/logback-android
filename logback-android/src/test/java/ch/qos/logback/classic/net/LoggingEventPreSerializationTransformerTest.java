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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;

/**
 * Unit tests for {@link LoggingEventPreSerializationTransformer}.
 */
public class LoggingEventPreSerializationTransformerTest {

  private final LoggingEventPreSerializationTransformer pst =
      new LoggingEventPreSerializationTransformer();

  @Test
  public void transformsNullToNull() {
    assertNull(pst.transform(null));
  }

  @Test
  public void transformsLoggingEventToEquivalentValueObject() {
    Logger logger = new LoggerContext().getLogger("pst");
    LoggingEvent event = new LoggingEvent(getClass().getName(), logger, Level.WARN,
        "hello {}", null, new Object[] {"world"});

    Serializable transformed = pst.transform(event);

    assertTrue(transformed instanceof LoggingEventVO);
    LoggingEventVO vo = (LoggingEventVO) transformed;
    assertEquals("pst", vo.getLoggerName());
    assertEquals(Level.WARN, vo.getLevel());
    assertEquals("hello world", vo.getFormattedMessage());
    assertEquals(event.getTimeStamp(), vo.getTimeStamp());
  }

  @Test
  public void passesValueObjectThroughUnchanged() {
    Logger logger = new LoggerContext().getLogger("pst");
    LoggingEventVO vo = LoggingEventVO.build(new LoggingEvent(getClass().getName(), logger,
        Level.INFO, "msg", null, null));

    assertSame(vo, pst.transform(vo));
  }

  @Test
  public void rejectsOtherEventTypes() {
    final ILoggingEvent other = mock(ILoggingEvent.class);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            pst.transform(other);
          }
        });

    assertEquals("Unsupported type " + other.getClass().getName(), ex.getMessage());
  }
}
