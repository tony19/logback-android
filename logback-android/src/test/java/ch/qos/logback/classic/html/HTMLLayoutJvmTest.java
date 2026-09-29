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
package ch.qos.logback.classic.html;

import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.html.IThrowableRenderer;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Plain-JVM tests of {@link HTMLLayout}; the XHTML-validating tests live in the
 * Robolectric-run {@link HTMLLayoutTest}.
 */
public class HTMLLayoutJvmTest {

  private LoggerContext lc;
  private Logger logger;
  private HTMLLayout layout;

  @Before
  public void setUp() {
    lc = new LoggerContext();
    logger = lc.getLogger("html.jvm");
    layout = new HTMLLayout();
    layout.setContext(lc);
  }

  @Test
  public void defaultsToFullPatternDefaultCssAndDefaultThrowableRenderer() {
    assertEquals("%date%thread%level%logger%mdc%msg", layout.getPattern());
    assertTrue(layout.getCssBuilder() instanceof DefaultCssBuilder);
    assertTrue(layout.getThrowableRenderer() instanceof DefaultThrowableRenderer);
  }

  @Test
  public void startsWhenThrowableRendererIsSet() {
    layout.start();

    assertTrue(layout.isStarted());
    new StatusChecker(lc).assertIsErrorFree();
  }

  @Test
  public void refusesToStartWithoutThrowableRenderer() {
    layout.setThrowableRenderer(null);
    assertNull(layout.getThrowableRenderer());

    layout.start();

    assertFalse(layout.isStarted());
    new StatusChecker(lc).assertContainsMatch(Status.ERROR, "ThrowableRender cannot be null.");
  }

  @Test
  public void consecutiveRowsAlternateBetweenEvenAndOdd() {
    layout.setPattern("%msg");
    layout.start();

    assertEquals(LINE_SEPARATOR + "<tr class=\"info even\">" + LINE_SEPARATOR
        + "<td class=\"Message\">one</td>" + LINE_SEPARATOR
        + "</tr>" + LINE_SEPARATOR, layout.doLayout(event(Level.INFO, "one")));
    assertEquals(LINE_SEPARATOR + "<tr class=\"warn odd\">" + LINE_SEPARATOR
        + "<td class=\"Message\">two</td>" + LINE_SEPARATOR
        + "</tr>" + LINE_SEPARATOR, layout.doLayout(event(Level.WARN, "two")));
    assertTrue(layout.doLayout(event(Level.ERROR, "three")).contains("<tr class=\"error even\">"));
  }

  @Test
  public void mdcColumnIsNamedAfterItsKeyOrMdcWhenNoKeyIsGiven() {
    layout.setPattern("%mdc{user}%mdc");
    layout.start();

    String header = layout.getPresentationHeader();
    assertTrue(header, header.endsWith("<tr class=\"header\">" + LINE_SEPARATOR
        + "<td class=\"user\">user</td>" + LINE_SEPARATOR
        + "<td class=\"MDC\">MDC</td>" + LINE_SEPARATOR
        + "</tr>" + LINE_SEPARATOR));

    LoggingEvent e = event(Level.INFO, "msg");
    e.setMDCPropertyMap(Collections.singletonMap("user", "alice"));
    String row = layout.doLayout(e);
    assertTrue(row, row.contains("<td class=\"user\">alice</td>" + LINE_SEPARATOR
        + "<td class=\"MDC\">user=alice</td>"));
  }

  @Test
  public void configuredThrowableRendererRendersEventsCarryingAThrowable() {
    layout.setPattern("%msg");
    TaggingThrowableRenderer renderer = new TaggingThrowableRenderer();
    layout.setThrowableRenderer(renderer);
    assertSame(renderer, layout.getThrowableRenderer());
    layout.start();

    String withThrowable = layout.doLayout(new LoggingEvent(getClass().getName(), logger,
        Level.ERROR, "failed", new IllegalStateException("boom"), null));
    String withoutThrowable = layout.doLayout(event(Level.INFO, "fine"));

    assertTrue(withThrowable, withThrowable.endsWith("</tr>" + LINE_SEPARATOR + "[boom]"));
    assertTrue(withoutThrowable, withoutThrowable.endsWith("</tr>" + LINE_SEPARATOR));
  }

  static class TaggingThrowableRenderer implements IThrowableRenderer<ILoggingEvent> {
    @Override
    public void render(StringBuilder sbuf, ILoggingEvent event) {
      sbuf.append('[').append(event.getThrowableProxy().getMessage()).append(']');
    }
  }

  private LoggingEvent event(Level level, String msg) {
    return new LoggingEvent(getClass().getName(), logger, level, msg, null, null);
  }
}
