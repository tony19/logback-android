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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

public class COWArrayListTest {

    Integer[] model = new Integer[0];
    COWArrayList<Integer> cowaList = new COWArrayList<Integer>(model);

    @Test
    public void basicToArray() {
        cowaList.add(1);
        Object[] result = cowaList.toArray();
        assertArrayEquals(new Integer[] { 1 }, result);
    }

    @Test
    public void basicToArrayWithModel() {
        cowaList.add(1);
        Integer[] result = cowaList.toArray(model);
        assertArrayEquals(new Integer[] { 1 }, result);
    }


    @Test
    public void basicToArrayTyped() {
        cowaList.add(1);
        Integer[] result = cowaList.asTypedArray();
        assertArrayEquals(new Integer[] { 1 }, result);
    }

    @Test
    public void sizeIsEmptyAndContainsReflectTheUnderlyingList() {
        assertEquals(0, cowaList.size());
        assertTrue(cowaList.isEmpty());
        assertFalse(cowaList.contains(1));

        cowaList.add(1);
        cowaList.add(2);

        assertEquals(2, cowaList.size());
        assertFalse(cowaList.isEmpty());
        assertTrue(cowaList.contains(2));
        assertFalse(cowaList.contains(3));
    }

    @Test
    public void containsAllChecksEveryElement() {
        cowaList.addAll(Arrays.asList(1, 2, 3));
        assertTrue(cowaList.containsAll(Arrays.asList(1, 3)));
        assertFalse(cowaList.containsAll(Arrays.asList(1, 4)));
    }

    @Test
    public void addAllAppendsAndRefreshesTheTypedArray() {
        cowaList.add(1);
        assertArrayEquals(new Integer[] { 1 }, cowaList.asTypedArray());

        assertTrue(cowaList.addAll(Arrays.asList(2, 3)));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, cowaList.asTypedArray());

        assertFalse(cowaList.addAll(Collections.<Integer>emptyList()));
    }

    @Test
    public void addAllAtIndexInsertsAndRefreshesTheTypedArray() {
        cowaList.addAll(Arrays.asList(1, 4));
        assertArrayEquals(new Integer[] { 1, 4 }, cowaList.asTypedArray());

        assertTrue(cowaList.addAll(1, Arrays.asList(2, 3)));
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, cowaList.asTypedArray());
    }

    @Test
    public void removeAllRemovesAndRefreshesTheTypedArray() {
        cowaList.addAll(Arrays.asList(1, 2, 3));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, cowaList.asTypedArray());

        assertTrue(cowaList.removeAll(Arrays.asList(1, 3)));
        assertArrayEquals(new Integer[] { 2 }, cowaList.asTypedArray());
        assertFalse(cowaList.removeAll(Arrays.asList(7)));
    }

    @Test
    public void retainAllRetainsAndRefreshesTheTypedArray() {
        cowaList.addAll(Arrays.asList(1, 2, 3));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, cowaList.asTypedArray());

        assertTrue(cowaList.retainAll(Arrays.asList(2, 3)));
        assertArrayEquals(new Integer[] { 2, 3 }, cowaList.asTypedArray());
        assertFalse(cowaList.retainAll(Arrays.asList(2, 3)));
    }

    @Test
    public void getReadsFromTheRefreshedCopy() {
        cowaList.add(1);
        assertEquals(Integer.valueOf(1), cowaList.get(0));

        cowaList.add(0, 5);
        assertEquals(Integer.valueOf(5), cowaList.get(0));
        assertEquals(Integer.valueOf(1), cowaList.get(1));
    }

    @Test
    public void setReplacesReturnsPreviousAndRefreshesTheTypedArray() {
        cowaList.addAll(Arrays.asList(1, 2));
        assertArrayEquals(new Integer[] { 1, 2 }, cowaList.asTypedArray());

        assertEquals(Integer.valueOf(2), cowaList.set(1, 9));
        assertArrayEquals(new Integer[] { 1, 9 }, cowaList.asTypedArray());
    }

    @Test
    public void addAtIndexInsertsAndRefreshesTheTypedArray() {
        cowaList.addAll(Arrays.asList(1, 3));
        assertArrayEquals(new Integer[] { 1, 3 }, cowaList.asTypedArray());

        cowaList.add(1, 2);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, cowaList.asTypedArray());
    }

    @Test
    public void removeAtIndexReturnsElementAndRefreshesTheTypedArray() {
        cowaList.addAll(Arrays.asList(1, 2, 3));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, cowaList.asTypedArray());

        assertEquals(Integer.valueOf(1), cowaList.remove(0));
        assertArrayEquals(new Integer[] { 2, 3 }, cowaList.asTypedArray());
    }

    @Test
    public void indexOfAndLastIndexOfSearchTheUnderlyingList() {
        cowaList.addAll(Arrays.asList(7, 8, 7));
        assertEquals(0, cowaList.indexOf(7));
        assertEquals(2, cowaList.lastIndexOf(7));
        assertEquals(-1, cowaList.indexOf(9));
        assertEquals(-1, cowaList.lastIndexOf(9));
    }

    @Test
    public void iteratorsWalkTheUnderlyingList() {
        cowaList.addAll(Arrays.asList(1, 2, 3));

        Iterator<Integer> it = cowaList.iterator();
        assertEquals(Integer.valueOf(1), it.next());

        ListIterator<Integer> li = cowaList.listIterator();
        assertFalse(li.hasPrevious());
        assertEquals(Integer.valueOf(1), li.next());

        ListIterator<Integer> fromIndex = cowaList.listIterator(2);
        assertEquals(2, fromIndex.nextIndex());
        assertEquals(Integer.valueOf(3), fromIndex.next());
        assertFalse(fromIndex.hasNext());
    }

    @Test
    public void subListViewsTheRequestedRange() {
        cowaList.addAll(Arrays.asList(1, 2, 3, 4));
        List<Integer> sub = cowaList.subList(1, 3);
        assertEquals(Arrays.asList(2, 3), sub);
    }

    @Test
    public void addIfAbsentAndClearRefreshTheTypedArray() {
        cowaList.addIfAbsent(1);
        cowaList.addIfAbsent(1);
        assertArrayEquals(new Integer[] { 1 }, cowaList.asTypedArray());

        cowaList.clear();
        assertArrayEquals(new Integer[0], cowaList.asTypedArray());
    }

    @Test
    public void removeObjectRefreshesTheTypedArray() {
        cowaList.addAll(Arrays.asList(1, 2));
        assertArrayEquals(new Integer[] { 1, 2 }, cowaList.asTypedArray());

        assertTrue(cowaList.remove(Integer.valueOf(1)));
        assertArrayEquals(new Integer[] { 2 }, cowaList.asTypedArray());
        assertFalse(cowaList.remove(Integer.valueOf(1)));
    }

}
