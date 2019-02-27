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
package ch.qos.logback.core.status;

import java.util.Iterator;

import junit.framework.TestCase;

public class StatusBaseTest extends TestCase {

  public void testAddStatus() {
    {
      InfoStatus status = new InfoStatus("testing", this);
      status.add(new ErrorStatus("error", this));
      Iterator<Status> it = status.iterator();
      assertTrue("No status was added", it.hasNext());
      assertTrue("hasChilden method reported wrong result", status
          .hasChildren());
    }
    {
      InfoStatus status = new InfoStatus("testing", this);
      try {
        status.add(null);
        fail("method should have thrown an Exception");
      } catch (NullPointerException ex) {
      }
    }
  }

  public void testRemoveStatus() {
    {
      InfoStatus status = new InfoStatus("testing", this);
      ErrorStatus error = new ErrorStatus("error", this);
      status.add(error);
      boolean result = status.remove(error);
      Iterator<Status> it = status.iterator();
      assertTrue("Remove failed", result);
      assertFalse("No status was removed", it.hasNext());
      assertFalse("hasChilden method reported wrong result", status
          .hasChildren());
    }
    {
      InfoStatus status = new InfoStatus("testing", this);
      ErrorStatus error = new ErrorStatus("error", this);
      status.add(error);
      boolean result = status.remove(null);
      assertFalse("Remove result was not false", result);
    }
  }

  public void testEffectiveLevel() {
    {
      // effective level = 0 level deep
      ErrorStatus status = new ErrorStatus("error", this);
      WarnStatus warn = new WarnStatus("warning", this);
      status.add(warn);
      assertEquals("effective level misevaluated", status.getEffectiveLevel(),
          Status.ERROR);
    }

    {
      // effective level = 1 level deep
      InfoStatus status = new InfoStatus("info", this);
      WarnStatus warn = new WarnStatus("warning", this);
      status.add(warn);
      assertEquals("effective level misevaluated", status.getEffectiveLevel(),
          Status.WARN);
    }

    {
      // effective level = 2 levels deep
      InfoStatus status = new InfoStatus("info", this);
      WarnStatus warn = new WarnStatus("warning", this);
      ErrorStatus error = new ErrorStatus("error", this);
      status.add(warn);
      warn.add(error);
      assertEquals("effective level misevaluated", status.getEffectiveLevel(),
          Status.ERROR);
    }
  }

}
