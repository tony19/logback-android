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
package ch.qos.logback.core.helpers;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class CyclicBufferTest {


  void assertSize(CyclicBuffer<String> cb, int size) {
     assertEquals(size, cb.length());
  }
  @Test
  public void smoke() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(2);
    assertSize(cb, 0);
    cb.add("zero");
    assertSize(cb, 1);
    cb.add("one");
    assertSize(cb, 2);
    cb.add("two");
    assertSize(cb, 2);
    assertEquals("one", cb.get());
    assertSize(cb, 1);
    assertEquals("two",cb.get());
    assertSize(cb, 0);
  }


  @Test
  public void cloning() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(2);
    cb.add("zero");
    cb.add("one");

    CyclicBuffer<String> clone = new CyclicBuffer<String>(cb);
    assertSize(clone, 2);
    cb.clear();
    assertSize(cb, 0);

    List<String> witness = Arrays.asList("zero", "one");
    assertEquals(witness, clone.asList());

  }

  @Test
  public void resizeRejectsNegativeSize() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(2);
    cb.add("a");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> cb.resize(-1));
    assertEquals("Negative array size [-1] not allowed.", e.getMessage());
    // the buffer is left untouched
    assertEquals(2, cb.getMaxSize());
    assertEquals(Arrays.asList("a"), cb.asList());
  }

  @Test
  public void resizeToCurrentCapacityOfFullBufferKeepsBufferAsIs() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(2);
    cb.add("a");
    cb.add("b");
    cb.add("c");
    Object[] storage = cb.ea;

    cb.resize(2);

    assertSame(storage, cb.ea);
    assertEquals(2, cb.getMaxSize());
    assertEquals(Arrays.asList("b", "c"), cb.asList());
    cb.add("d");
    assertEquals(Arrays.asList("c", "d"), cb.asList());
  }

  @Test
  public void resizeToCurrentLengthShrinksCapacity() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(4);
    cb.add("a");
    cb.add("b");

    cb.resize(2);

    assertEquals(2, cb.getMaxSize());
    assertEquals(Arrays.asList("a", "b"), cb.asList());
    cb.add("c");
    assertEquals(Arrays.asList("b", "c"), cb.asList());
  }

  @Test
  public void growingAFullBufferKeepsElementsAndAppendsAfterThem() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(3);
    cb.add("a");
    cb.add("b");
    cb.add("c");
    cb.add("d");

    cb.resize(5);

    assertEquals(5, cb.getMaxSize());
    assertEquals(Arrays.asList("b", "c", "d"), cb.asList());
    cb.add("e");
    cb.add("f");
    assertEquals(Arrays.asList("b", "c", "d", "e", "f"), cb.asList());
    cb.add("g");
    assertEquals(Arrays.asList("c", "d", "e", "f", "g"), cb.asList());
  }

  @Test
  public void shrinkingAFullBufferKeepsTheOldestElements() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(3);
    cb.add("a");
    cb.add("b");
    cb.add("c");
    cb.add("d");

    cb.resize(2);

    assertEquals(2, cb.getMaxSize());
    assertEquals(Arrays.asList("b", "c"), cb.asList());
    cb.add("e");
    assertEquals(Arrays.asList("c", "e"), cb.asList());
  }

  @Test
  public void resizeAfterRemovalKeepsRemainingElementsInOrder() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(4);
    cb.add("a");
    cb.add("b");
    cb.add("c");
    assertEquals("a", cb.get());

    cb.resize(3);

    assertEquals(3, cb.getMaxSize());
    assertEquals(Arrays.asList("b", "c"), cb.asList());
    cb.add("d");
    assertEquals(Arrays.asList("b", "c", "d"), cb.asList());
  }

  @Test
  public void resizeOfWrappedPartiallyFilledBufferKeepsRemainingElementsInOrder() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(4);
    cb.add("a");
    cb.add("b");
    cb.add("c");
    cb.add("d");
    cb.add("e"); // overwrites "a", the oldest element is now at index 1
    assertEquals("b", cb.get());
    assertEquals("c", cb.get());
    // "d" is stored at index 3 and "e" at index 0

    cb.resize(3);

    assertEquals(Arrays.asList("d", "e"), cb.asList());
  }

  @Test
  public void resizeToZeroEmptiesTheBuffer() {
    CyclicBuffer<String> cb = new CyclicBuffer<String>(3);
    cb.add("a");

    cb.resize(0);

    assertEquals(0, cb.getMaxSize());
    assertSize(cb, 0);
    assertNull(cb.get());
  }
}
