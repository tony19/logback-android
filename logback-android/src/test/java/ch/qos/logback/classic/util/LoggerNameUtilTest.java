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
package ch.qos.logback.classic.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LoggerNameUtilTest {


  @Test
  public void smoke0() {
    List<String> witnessList = new ArrayList<String>();
    witnessList.add("a");
    witnessList.add("b");
    witnessList.add("c");
    List<String> partList = computeNameParts("a.b.c");
    assertEquals(witnessList, partList);
  }

  @Test
  public void smoke1() {
    List<String> witnessList = new ArrayList<String>();
    witnessList.add("com");
    witnessList.add("foo");
    witnessList.add("Bar");
    List<String> partList = computeNameParts("com.foo.Bar");
    assertEquals(witnessList, partList);
  }

  @Test
  public void emptyStringShouldReturnAListContainingOneEmptyString() {
    List<String> witnessList = new ArrayList<String>();
    witnessList.add("");
    List<String> partList = computeNameParts("");
    assertEquals(witnessList, partList);
  }

  @Test
  public void dotAtLastPositionShouldReturnAListWithAnEmptyStringAsLastElement() {
    List<String> witnessList = new ArrayList<String>();
    witnessList.add("com");
    witnessList.add("foo");
    witnessList.add("");

    List<String> partList = computeNameParts("com.foo.");
    assertEquals(witnessList, partList);
  }

  @Test
  public void supportNestedClasses() {
    List<String> witnessList = new ArrayList<String>();
    witnessList.add("com");
    witnessList.add("foo");
    witnessList.add("Bar");
    witnessList.add("Nested");

    List<String> partList = computeNameParts("com.foo.Bar$Nested");
    assertEquals(witnessList, partList);
  }

  @Test
  public void supportNestedClassesWithNestedDot() {
    //LOGBACK-384
    List<String> witnessList = new ArrayList<String>();
    witnessList.add("com");
    witnessList.add("foo");
    witnessList.add("Bar");
    witnessList.add("Nested");
    witnessList.add("dot");

    List<String> partList = computeNameParts("com.foo.Bar$Nested.dot");
    assertEquals(witnessList, partList);
  }

  @Test
  public void supportNestedClassesAtBeginning() {
    List<String> witnessList = new ArrayList<String>();
    witnessList.add("foo");
    witnessList.add("Nested");
    witnessList.add("bar");

    List<String> partList = computeNameParts("foo$Nested.bar");
    assertEquals(witnessList, partList);
  }

  private List<String> computeNameParts(String loggerName) {
    List<String> partList = new ArrayList<String>();

    int fromIndex = 0;
    while(true) {
      int index = LoggerNameUtil.getSeparatorIndexOf(loggerName, fromIndex);
      if(index == -1) {
       partList.add(loggerName.substring(fromIndex));
       break;
      }
      partList.add(loggerName.substring(fromIndex, index));
      fromIndex = index+1;
    }
    return partList;
  }
}
