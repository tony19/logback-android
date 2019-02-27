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
package ch.qos.logback.core.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;


/**
 * Unit tests for {@link StringCollectionUtil}.
 *
 * @author Carl Harris
 */
public class StringCollectionUtilTest {

  @Test
  public void testRetainMatchingWithNoPatterns() throws Exception {
    Collection<String> values = stringToList("A");
    StringCollectionUtil.retainMatching(values);
    assertTrue(values.contains("A"));
  }

  @Test
  public void testRetainMatchingWithMatchingPattern() throws Exception {
    Collection<String> values = stringToList("A");
    StringCollectionUtil.retainMatching(values, "A");
    assertTrue(values.contains("A"));
  }

  @Test
  public void testRetainMatchingWithNoMatchingPattern() throws Exception {
    Collection<String> values = stringToList("A");
    StringCollectionUtil.retainMatching(values, "B");
    assertTrue(values.isEmpty());
  }

  @Test
  public void testRemoveMatchingWithNoPatterns() throws Exception {
    Collection<String> values = stringToList("A");
    StringCollectionUtil.removeMatching(values);
    assertTrue(values.contains("A"));
  }

  @Test
  public void testRemoveMatchingWithMatchingPattern() throws Exception {
    Collection<String> values = stringToList("A");
    StringCollectionUtil.removeMatching(values, "A");
    assertTrue(values.isEmpty());
  }

  @Test
  public void testRemoveMatchingWithNoMatchingPattern() throws Exception {
    Collection<String> values = stringToList("A");
    StringCollectionUtil.removeMatching(values, "B");
    assertTrue(values.contains("A"));
  }

  private List<String> stringToList(String... values) {
    List<String> result = new ArrayList<String>(values.length);
    result.addAll(Arrays.asList(values));
    return result;
  }

}
