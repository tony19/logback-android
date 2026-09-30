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
package ch.qos.logback.core.joran.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HostClassAndPropertyDoubleTest {

  private static HostClassAndPropertyDouble hcpd(Class<?> hostClass, String propertyName) {
    return new HostClassAndPropertyDouble(hostClass, propertyName);
  }

  @Test
  public void gettersReturnConstructorArguments() {
    HostClassAndPropertyDouble d = hcpd(String.class, "encoder");
    assertSame(String.class, d.getHostClass());
    assertEquals("encoder", d.getPropertyName());

    HostClassAndPropertyDouble empty = hcpd(null, null);
    assertNull(empty.getHostClass());
    assertNull(empty.getPropertyName());
  }

  @Test
  public void equalByHostClassAndPropertyName() {
    HostClassAndPropertyDouble d = hcpd(String.class, "encoder");

    assertTrue(d.equals(d));
    assertEquals(d, hcpd(String.class, "encoder"));
    assertEquals(d.hashCode(), hcpd(String.class, "encoder").hashCode());
    assertNotEquals(d, hcpd(Integer.class, "encoder"));
    assertNotEquals(d, hcpd(String.class, "layout"));
    assertNotEquals(d, hcpd(String.class, "Encoder"));
  }

  @Test
  public void neverEqualToNullOrOtherTypes() {
    HostClassAndPropertyDouble d = hcpd(String.class, "encoder");
    assertFalse(d.equals(null));
    assertFalse(d.equals("encoder"));
  }

  @Test
  public void nullMembersAreOnlyEqualToNullMembers() {
    assertEquals(hcpd(null, null), hcpd(null, null));
    assertEquals(hcpd(null, "p"), hcpd(null, "p"));
    assertEquals(hcpd(String.class, null), hcpd(String.class, null));

    assertNotEquals(hcpd(null, "p"), hcpd(String.class, "p"));
    assertNotEquals(hcpd(String.class, "p"), hcpd(null, "p"));
    assertNotEquals(hcpd(String.class, null), hcpd(String.class, "p"));
    assertNotEquals(hcpd(String.class, "p"), hcpd(String.class, null));
  }

  @Test
  public void hashCodeTreatsNullMembersAsZero() {
    assertEquals(31 * 31, hcpd(null, null).hashCode());
    assertEquals(31 * (31 + String.class.hashCode()), hcpd(String.class, null).hashCode());
    assertEquals(31 * 31 + "p".hashCode(), hcpd(null, "p").hashCode());
  }
}
