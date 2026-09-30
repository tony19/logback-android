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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.helper.FileNamePattern;

/**
 * Unit tests for {@link ContextUtil}.
 */
public class ContextUtilTest {

  Context context = new ContextBase();
  ContextUtil contextUtil = new ContextUtil(context);

  @Test
  public void addHostNameAsPropertyPutsLocalhost() {
    contextUtil.addHostNameAsProperty();
    assertEquals("localhost", context.getProperty(CoreConstants.HOSTNAME_KEY));
  }

  @Test
  public void addPropertiesCopiesEveryPropertyIntoTheContext() {
    Properties props = new Properties();
    props.setProperty("ctxUtilKeyA", "valueA");
    props.setProperty("ctxUtilKeyB", "valueB");

    contextUtil.addProperties(props);

    Map<String, String> expected = new HashMap<String, String>();
    expected.put("ctxUtilKeyA", "valueA");
    expected.put("ctxUtilKeyB", "valueB");
    assertEquals(expected, context.getCopyOfPropertyMap());
  }

  @Test
  public void addPropertiesIgnoresNull() {
    contextUtil.addProperties(null);
    assertEquals(new HashMap<String, String>(), context.getCopyOfPropertyMap());
  }

  @Test
  public void filenameCollisionMapIsReadFromTheContext() {
    Map<String, String> map = new HashMap<String, String>();
    context.putObject(CoreConstants.FA_FILENAME_COLLISION_MAP, map);
    assertSame(map, ContextUtil.getFilenameCollisionMap(context));
  }

  @Test
  public void filenameCollisionMapOfNullContextIsNull() {
    assertNull(ContextUtil.getFilenameCollisionMap(null));
  }

  @Test
  public void filenamePatternCollisionMapIsReadFromTheContext() {
    Map<String, FileNamePattern> map = new HashMap<String, FileNamePattern>();
    context.putObject(CoreConstants.RFA_FILENAME_PATTERN_COLLISION_MAP, map);
    assertSame(map, ContextUtil.getFilenamePatternCollisionMap(context));
  }

  @Test
  public void filenamePatternCollisionMapOfNullContextIsNull() {
    assertNull(ContextUtil.getFilenamePatternCollisionMap(null));
  }
}
