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
package ch.qos.logback.classic.boolex;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.MarkerFactory;

import java.util.Collections;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;


public class OnMarkerEvaluatorTest {

  
  LoggerContext lc = new LoggerContext();
  LoggingEvent event = makeEvent();
  OnMarkerEvaluator evaluator = new OnMarkerEvaluator();

  @Before
  public void before() {
    evaluator.setContext(lc);
  }
  
  @Test
  public void smoke() throws EvaluationException {
    evaluator.addMarker("M");
    evaluator.start();
   
    event.setMarkers(Collections.singletonList(MarkerFactory.getMarker("M")));
    assertTrue(evaluator.evaluate(event));
  }
  
  @Test
  public void nullMarkerInEvent() throws EvaluationException {
    evaluator.addMarker("M");
    evaluator.start();
    assertFalse(evaluator.evaluate(event));
  }
  
  @Test
  public void nullMarkerInEvaluator() throws EvaluationException {
    evaluator.addMarker("M");
    evaluator.start();
    assertFalse(evaluator.evaluate(event));
  }
  
  
  LoggingEvent makeEvent() {
    return new LoggingEvent("x", lc.getLogger("x"), Level.DEBUG, "msg", null, null);
  }
}
