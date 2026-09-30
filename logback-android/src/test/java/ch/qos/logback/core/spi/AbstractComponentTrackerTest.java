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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Tests the parts of {@link AbstractComponentTracker} that are not exercised
 * through its concrete subclasses: stale components, {@code allComponents()},
 * the timeout/max-components getters and the internal entry's
 * {@code equals}/{@code hashCode}/{@code toString}.
 */
public class AbstractComponentTrackerTest {

  long now = 3000;

  @Test
  public void timeoutAndMaxComponentsHaveDefaultsAndCanBeChanged() {
    StringComponentTracker tracker = new StringComponentTracker();
    assertEquals(ComponentTracker.DEFAULT_TIMEOUT, tracker.getTimeout());
    assertEquals(ComponentTracker.DEFAULT_MAX_COMPONENTS, tracker.getMaxComponents());

    tracker.setTimeout(1234);
    tracker.setMaxComponents(7);

    assertEquals(1234, tracker.getTimeout());
    assertEquals(7, tracker.getMaxComponents());
  }

  @Test
  public void staleComponentIsRemovedBeforeItsTimeoutExpires() {
    StringComponentTracker tracker = new StringComponentTracker();
    String stale = tracker.getOrCreate("stale", now);
    String fresh = tracker.getOrCreate("fresh", now);
    tracker.staleComponents.add(stale);

    // well within the (default, 30 minutes) timeout
    tracker.removeStaleComponents(now + 1);

    assertNull(tracker.find("stale"));
    assertSame(fresh, tracker.find("fresh"));
    assertEquals(Collections.singletonList(stale), tracker.removedComponents);
    assertEquals(1, tracker.getComponentCount());
  }

  @Test
  public void nonStaleComponentIsKeptUntilItsTimeoutExpires() {
    StringComponentTracker tracker = new StringComponentTracker();
    String component = tracker.getOrCreate("k", now);

    tracker.removeStaleComponents(now + ComponentTracker.DEFAULT_TIMEOUT);
    assertSame(component, tracker.find("k"));
    assertTrue(tracker.removedComponents.isEmpty());

    // the next removal iteration may only run WAIT_BETWEEN_SUCCESSIVE_REMOVAL_ITERATIONS later
    tracker.removeStaleComponents(now + ComponentTracker.DEFAULT_TIMEOUT
        + AbstractComponentTracker.WAIT_BETWEEN_SUCCESSIVE_REMOVAL_ITERATIONS);
    assertNull(tracker.find("k"));
    assertEquals(Collections.singletonList(component), tracker.removedComponents);
  }

  @Test
  public void allComponentsReturnsLiveAndLingeringComponents() {
    StringComponentTracker tracker = new StringComponentTracker();
    String live1 = tracker.getOrCreate("live1", now);
    String live2 = tracker.getOrCreate("live2", now);
    String lingering = tracker.getOrCreate("lingering", now);
    tracker.endOfLife("lingering");

    assertThat(tracker.allComponents(), containsInAnyOrder(live1, live2, lingering));
    assertEquals(new HashSet<String>(Arrays.asList("live1", "live2", "lingering")),
        tracker.allKeys());
  }

  @Test
  public void allComponentsOfAnEmptyTrackerIsEmpty() {
    assertTrue(new StringComponentTracker().allComponents().isEmpty());
  }

  @Test
  public void allComponentsReturnsASnapshot() {
    StringComponentTracker tracker = new StringComponentTracker();
    String component = tracker.getOrCreate("k", now);
    List<String> snapshot = new ArrayList<String>(tracker.allComponents());

    tracker.allComponents().clear();

    assertEquals(Collections.singletonList(component), snapshot);
    assertSame(component, tracker.find("k"));
  }

  @Test
  public void entryToStringShowsKeyAndComponent() {
    StringComponentTracker tracker = new StringComponentTracker();
    tracker.getOrCreate("k", now);

    assertEquals("(k, component-k)", entryOf(tracker, "k").toString());
  }

  @Test
  public void entryHashCodeIsTheHashCodeOfItsKey() {
    StringComponentTracker tracker = new StringComponentTracker();
    tracker.getOrCreate("some key", now);

    assertEquals("some key".hashCode(), entryOf(tracker, "some key").hashCode());
  }

  @Test
  public void entryEqualsItselfButNotNullNorOtherTypes() {
    StringComponentTracker tracker = new StringComponentTracker();
    tracker.getOrCreate("k", now);
    Object entry = entryOf(tracker, "k");

    assertTrue(entry.equals(entry));
    assertFalse(entry.equals(null));
    assertFalse(entry.equals("(k, component-k)"));
  }

  @Test
  public void entriesWithSameKeyAndComponentAreEqualWhateverTheirTimestamp() {
    Object entry = entry("k", "c", now);
    Object other = entry("k", "c", now + 10);

    assertEquals(entry, other);
    assertEquals(entry.hashCode(), other.hashCode());
  }

  @Test
  public void entriesWithDifferentKeysAreNotEqual() {
    assertNotEquals(entry("k1", "c", now), entry("k2", "c", now));
  }

  @Test
  public void entriesWithDifferentComponentsAreNotEqual() {
    assertNotEquals(entry("k", "c1", now), entry("k", "c2", now));
  }

  @Test
  public void entryWithNullKeyEqualsOnlyEntriesWithNullKey() {
    Object nullKeyEntry = entry(null, "c", now);

    assertNotEquals(nullKeyEntry, entry("k", "c", now));
    assertEquals(nullKeyEntry, entry(null, "c", now));
    // and the other way around
    assertNotEquals(entry("k", "c", now), nullKeyEntry);
  }

  @Test
  public void entryWithNullComponentEqualsOnlyEntriesWithNullComponent() {
    Object nullComponentEntry = entry("k", null, now);

    assertNotEquals(nullComponentEntry, entry("k", "c", now));
    assertEquals(nullComponentEntry, entry("k", null, now));
    // and the other way around
    assertNotEquals(entry("k", "c", now), nullComponentEntry);
  }

  /**
   * Builds, through the public API of a fresh tracker, the internal entry
   * mapping {@code key} to {@code component}.
   */
  private static Object entry(String key, String component, long timestamp) {
    StringComponentTracker tracker = new StringComponentTracker();
    tracker.componentsToBuild.put(key, component);
    assertEquals(component, tracker.getOrCreate(key, timestamp));
    return entryOf(tracker, key);
  }

  private static Object entryOf(AbstractComponentTracker<?> tracker, String key) {
    assertTrue(tracker.liveMap.containsKey(key));
    return tracker.liveMap.get(key);
  }

  /**
   * A tracker of strings: the component built for a key is looked up in
   * {@link #componentsToBuild}, and defaults to "component-" + key.
   */
  static class StringComponentTracker extends AbstractComponentTracker<String> {
    final Map<String, String> componentsToBuild = new HashMap<String, String>();
    final Set<String> staleComponents = new HashSet<String>();
    final List<String> removedComponents = new ArrayList<String>();

    @Override
    protected void processPriorToRemoval(String component) {
      removedComponents.add(component);
    }

    @Override
    protected String buildComponent(String key) {
      if (componentsToBuild.containsKey(key)) {
        return componentsToBuild.get(key);
      }
      return "component-" + key;
    }

    @Override
    protected boolean isComponentStale(String component) {
      return staleComponents.contains(component);
    }
  }
}
