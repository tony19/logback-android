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
package ch.qos.logback.classic;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.turbo.MarkerFilter;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.List;


public class TurboFilteringInLoggerTest  {

  static final String BLUE = "BLUE";
  LoggerContext context;
  Logger logger;
  Marker blueMarker = MarkerFactory.getMarker(BLUE);

  @Before
  public void setUp() throws Exception {
    context = new LoggerContext();
    context.setName("test");
    context.start();
    logger = context.getLogger(TurboFilteringInLoggerTest.class);
  }

 
  private void addYesFilter() {
    YesFilter filter = new YesFilter();
    filter.start();
    context.addTurboFilter(filter);
  }

  private void addNoFilter() {
    NoFilter filter = new NoFilter();
    filter.start();
    context.addTurboFilter(filter);
  }

  private void addAcceptBLUEFilter() {
    MarkerFilter filter = new MarkerFilter();
    filter.setMarker(BLUE);
    filter.setOnMatch("ACCEPT");
    filter.start();
    context.addTurboFilter(filter);
  }

  private void addDenyBLUEFilter() {
    MarkerFilter filter = new MarkerFilter();
    filter.setMarker(BLUE);
    filter.setOnMatch("DENY");
    filter.start();
    context.addTurboFilter(filter);
  }

  @Test
  public void testIsDebugEnabledWithYesFilter() {
    addYesFilter();
    logger.setLevel(Level.INFO);
    assertTrue(logger.isDebugEnabled());
  }

  @Test
  public void testIsInfoEnabledWithYesFilter() {
    addYesFilter();
    logger.setLevel(Level.WARN);
    assertTrue(logger.isInfoEnabled());
  }

  @Test
  public void testIsWarnEnabledWithYesFilter() {
    addYesFilter();
    logger.setLevel(Level.ERROR);
    assertTrue(logger.isWarnEnabled());
  }

  @Test
  public void testIsErrorEnabledWithYesFilter() {
    addYesFilter();
    logger.setLevel(Level.OFF);
    assertTrue(logger.isErrorEnabled());
  }

  @Test
  public void testIsEnabledForWithYesFilter() {
    addYesFilter();
    logger.setLevel(Level.ERROR);
    assertTrue(logger.isEnabledFor(Level.INFO));
  }

  @Test
  public void testIsEnabledForWithNoFilter() {
    addNoFilter();
    logger.setLevel(Level.DEBUG);
    assertFalse(logger.isEnabledFor(Level.INFO));
  }

  @Test
  public void testIsDebugEnabledWithNoFilter() {
    addNoFilter();
    logger.setLevel(Level.DEBUG);
    assertFalse(logger.isDebugEnabled());
  }

  @Test
  public void testIsInfoEnabledWithNoFilter() {
    addNoFilter();
    logger.setLevel(Level.DEBUG);
    assertFalse(logger.isInfoEnabled());
  }

  @Test
  public void testIsWarnEnabledWithNoFilter() {
    addNoFilter();
    logger.setLevel(Level.DEBUG);
    assertFalse(logger.isWarnEnabled());
  }

  @Test
  public void testIsErrorEnabledWithNoFilter() {
    addNoFilter();
    logger.setLevel(Level.DEBUG);
    assertFalse(logger.isErrorEnabled());
  }

  @Test
  public void testIsErrorEnabledWithAcceptBlueFilter() {
    addAcceptBLUEFilter();
    logger.setLevel(Level.ERROR);
    assertTrue(logger.isDebugEnabled(blueMarker));
  }

  @Test
  public void testIsErrorEnabledWithDenyBlueFilter() {
    addDenyBLUEFilter();
    logger.setLevel(Level.ALL);
    assertFalse(logger.isDebugEnabled(blueMarker));
  }

  @Test
  public void testLoggingContextReset() {
    addYesFilter();
    assertNotNull(context.getTurboFilterList().get(0));
    context.reset();
    assertEquals(0, context.getTurboFilterList().size());
  }

}

class YesFilter extends TurboFilter {
  @Override
  public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format, Object[] params, Throwable t) {
    return FilterReply.ACCEPT;
  }
}

class NoFilter extends TurboFilter {
  @Override
  public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format, Object[] params, Throwable t) {
    return FilterReply.DENY;
  }
}