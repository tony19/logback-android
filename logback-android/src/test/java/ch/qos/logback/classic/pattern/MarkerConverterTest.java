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
package ch.qos.logback.classic.pattern;

import static junit.framework.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

import java.util.Collections;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

public class MarkerConverterTest {
  
  LoggerContext lc;
  MarkerConverter converter;
  // use a different facotry for each test so that they are independent
  IMarkerFactory markerFactory = new BasicMarkerFactory();
  
  @Before
  public void setUp() throws Exception {
    lc = new LoggerContext();
    converter = new MarkerConverter();
    converter.start();
  }
  
  @After
  public void tearDown() throws Exception {
    lc = null;
    converter.stop();
    converter = null;
  }

  @Test
  public void testWithNullMarker() {
    String result = converter.convert(createLoggingEvent(null));
    assertEquals("[null]", result);
  }
  
  @Test
  public void testWithMarker() {
    String name = "test";
    Marker marker = markerFactory.getMarker(name);
    String result = converter.convert(createLoggingEvent(marker));
    assertEquals("[" + name + "]", result);
  }
  
  @Test
  public void testWithOneChildMarker() {
    Marker marker = markerFactory.getMarker("test");
    marker.add(markerFactory.getMarker("child"));
    
    String result = converter.convert(createLoggingEvent(marker));
    
    assertEquals("[test [ child ]]", result);
  }
  
  @Test
  public void testWithSeveralChildMarker() {
    Marker marker = markerFactory.getMarker("testParent");
    marker.add(markerFactory.getMarker("child1"));
    marker.add(markerFactory.getMarker("child2"));
    marker.add(markerFactory.getMarker("child3"));
    
    String result = converter.convert(createLoggingEvent(marker));
    
    assertEquals("[testParent [ child1, child2, child3 ]]", result);
  }
  
  private ILoggingEvent createLoggingEvent(Marker marker) {
    LoggingEvent le = new LoggingEvent(this.getClass().getName(), lc.getLogger(Logger.ROOT_LOGGER_NAME),
        Level.DEBUG, "test message", null, null);
    le.setMarkers(Collections.singletonList(marker));
    return le;
  }
}
