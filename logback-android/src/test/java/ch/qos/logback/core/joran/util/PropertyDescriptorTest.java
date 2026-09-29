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
package ch.qos.logback.core.joran.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.Test;

public class PropertyDescriptorTest {

  @Test
  public void newDescriptorHasOnlyAName() {
    PropertyDescriptor pd = new PropertyDescriptor("level");
    assertEquals("level", pd.getName());
    assertNull(pd.getReadMethod());
    assertNull(pd.getWriteMethod());
    assertNull(pd.getPropertyType());
  }

  @Test
  public void accessorsRoundTrip() throws Exception {
    Method read = StringBuilder.class.getMethod("length");
    Method write = StringBuilder.class.getMethod("setLength", int.class);
    PropertyDescriptor pd = new PropertyDescriptor("length");
    pd.setReadMethod(read);
    pd.setWriteMethod(write);
    pd.setPropertyType(int.class);
    assertEquals(read, pd.getReadMethod());
    assertEquals(write, pd.getWriteMethod());
    assertEquals(int.class, pd.getPropertyType());
  }
}
