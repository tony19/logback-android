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
package ch.qos.logback.classic.turbo;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.core.spi.FilterReply;

import java.util.Collections;

public class MarkerFilterTest {

  static String TOTO = "TOTO";
  static String COMPOSITE = "COMPOSITE";

  Marker totoMarker = MarkerFactory.getMarker(TOTO);


  @Test
  public void testNoMarker() {
    MarkerFilter mkt = new MarkerFilter();
    mkt.start();
    assertFalse(mkt.isStarted());
    assertEquals(FilterReply.NEUTRAL, mkt.decide(Collections.singletonList(totoMarker), null, null, null, null, null));
    assertEquals(FilterReply.NEUTRAL, mkt.decide(null, null, null, null, null, null));

  }


  @Test
  public void testBasic() {
    MarkerFilter mkt = new MarkerFilter();
    mkt.setMarker(TOTO);
    mkt.setOnMatch("ACCEPT");
    mkt.setOnMismatch("DENY");

    mkt.start();
    assertTrue(mkt.isStarted());
    assertEquals(FilterReply.DENY, mkt.decide(null, null, null, null, null, null));
    assertEquals(FilterReply.ACCEPT, mkt.decide(Collections.singletonList(totoMarker), null, null, null, null, null));
  }

  @Test
  public void testComposite() {
    String compositeMarkerName = COMPOSITE;
    Marker compositeMarker = MarkerFactory.getMarker(compositeMarkerName);
    compositeMarker.add(totoMarker);

    MarkerFilter mkt = new MarkerFilter();
    mkt.setMarker(TOTO);
    mkt.setOnMatch("ACCEPT");
    mkt.setOnMismatch("DENY");

    mkt.start();

    assertTrue(mkt.isStarted());
    assertEquals(FilterReply.DENY, mkt.decide(null, null, null, null, null, null));
    assertEquals(FilterReply.ACCEPT, mkt.decide(Collections.singletonList(totoMarker), null, null, null, null, null));
    assertEquals(FilterReply.ACCEPT, mkt.decide(Collections.singletonList(compositeMarker), null, null, null, null, null));
  }

}
