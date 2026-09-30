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
package ch.qos.logback.classic.pattern.color;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

public class HighlightingCompositeConverterTest {

  private static final String ESC = "\033[";
  private static final String RESET = ESC + "0;39m";

  private final LoggerContext context = new LoggerContext();
  private final PatternLayout layout = new PatternLayout();

  @Before
  public void setUp() {
    layout.setContext(context);
    layout.setPattern("%highlight(%msg)");
    layout.start();
  }

  private ILoggingEvent eventAt(Level level) {
    return new LoggingEvent(getClass().getName(), context.getLogger("x"), level, "hi", null, null);
  }

  @Test
  public void errorIsBoldRed() {
    assertEquals(ESC + "1;31m" + "hi" + RESET, layout.doLayout(eventAt(Level.ERROR)));
  }

  @Test
  public void warnIsRed() {
    assertEquals(ESC + "31m" + "hi" + RESET, layout.doLayout(eventAt(Level.WARN)));
  }

  @Test
  public void infoIsBlue() {
    assertEquals(ESC + "34m" + "hi" + RESET, layout.doLayout(eventAt(Level.INFO)));
  }

  @Test
  public void lowerLevelsUseTheDefaultColor() {
    assertEquals(ESC + "39m" + "hi" + RESET, layout.doLayout(eventAt(Level.DEBUG)));
    assertEquals(ESC + "39m" + "hi" + RESET, layout.doLayout(eventAt(Level.TRACE)));
  }
}
