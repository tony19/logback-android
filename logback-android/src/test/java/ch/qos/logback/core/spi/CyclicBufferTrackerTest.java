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
package ch.qos.logback.core.spi;

import ch.qos.logback.core.helpers.CyclicBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class CyclicBufferTrackerTest {


  CyclicBufferTracker<Object> tracker = new CyclicBufferTracker<Object>();
  String key = "a";

  @Test
  public void empty0() {
    long now = 3000;
    tracker.removeStaleComponents(now);
    assertEquals(0, tracker.liveKeysAsOrderedList().size());
    assertEquals(0, tracker.getComponentCount());
  }

  @Test
  public void empty1() {
    long now = 3000;
    assertNotNull(tracker.getOrCreate(key, now++));
    now += ComponentTracker.DEFAULT_TIMEOUT + 1000;
    tracker.removeStaleComponents(now);
    assertEquals(0, tracker.liveKeysAsOrderedList().size());
    assertEquals(0, tracker.getComponentCount());

    assertNotNull(tracker.getOrCreate(key, now++));
  }

  @Test
  public void smoke() {
    long now = 3000;
    CyclicBuffer<Object> cb = tracker.getOrCreate(key, now);
    assertEquals(cb, tracker.getOrCreate(key, now++));
    now += CyclicBufferTracker.DEFAULT_TIMEOUT + 1000;
    tracker.removeStaleComponents(now);
    assertEquals(0, tracker.liveKeysAsOrderedList().size());
    assertEquals(0, tracker.getComponentCount());
  }

  @Test
  public void destroy() {
    long now = 3000;
    CyclicBuffer<Object> cb = tracker.getOrCreate(key, now);
    cb.add(new Object());
    assertEquals(1, cb.length());
    tracker.endOfLife(key);
    now += CyclicBufferTracker.LINGERING_TIMEOUT + 10;
    tracker.removeStaleComponents(now);
    assertEquals(0, tracker.liveKeysAsOrderedList().size());
    assertEquals(0, tracker.getComponentCount());
    assertEquals(0, cb.length());
  }




}
