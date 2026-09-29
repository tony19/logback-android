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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.util.StatusPrinter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OnPrintStreamStatusListenerBaseTest {

  private static final long ONE_HOUR = 60L * 60L * 1000L;

  /** Prints to an in-memory stream so that the output can be inspected. */
  static class CapturingPrintStreamStatusListener extends OnPrintStreamStatusListenerBase {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final PrintStream printStream = newUtf8PrintStream(bytes);

    @Override
    protected PrintStream getPrintStream() {
      return printStream;
    }

    String output() {
      printStream.flush();
      try {
        return bytes.toString("UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  static PrintStream newUtf8PrintStream(ByteArrayOutputStream bytes) {
    try {
      return new PrintStream(bytes, true, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private final Context context = new ContextBase();
  private final CapturingPrintStreamStatusListener listener = new CapturingPrintStreamStatusListener();

  private static String render(Status status) {
    StringBuilder sb = new StringBuilder();
    StatusPrinter.buildStr(sb, "", status);
    return sb.toString();
  }

  @Test
  public void defaults() {
    assertNull(listener.getPrefix());
    assertEquals(OnPrintStreamStatusListenerBase.DEFAULT_RETROSPECTIVE, listener.getRetrospective());
    assertEquals(300, listener.getRetrospective());
    assertFalse(listener.isStarted());
  }

  @Test
  public void setPrefixIsReturnedByGetter() {
    listener.setPrefix("PFX ");

    assertEquals("PFX ", listener.getPrefix());
  }

  @Test
  public void setRetrospectiveIsReturnedByGetter() {
    listener.setRetrospective(1234);

    assertEquals(1234, listener.getRetrospective());
  }

  @Test
  public void statusEventsAreIgnoredBeforeStart() {
    listener.addStatusEvent(new InfoStatus("hello", "origin"));

    assertEquals("", listener.output());
  }

  @Test
  public void startedListenerPrintsIncomingStatusWithoutPrefixByDefault() {
    listener.start();
    InfoStatus status = new InfoStatus("hello", "origin");

    listener.addStatusEvent(status);

    assertTrue(listener.isStarted());
    assertEquals(render(status), listener.output());
  }

  @Test
  public void startedListenerPrintsPrefixBeforeEachStatus() {
    listener.setPrefix("PFX ");
    listener.start();
    InfoStatus first = new InfoStatus("first", "origin");
    WarnStatus second = new WarnStatus("second", "origin");

    listener.addStatusEvent(first);
    listener.addStatusEvent(second);

    assertEquals("PFX " + render(first) + "PFX " + render(second), listener.output());
  }

  @Test
  public void stopMakesTheListenerIgnoreFurtherStatusEvents() {
    listener.start();
    listener.stop();

    listener.addStatusEvent(new InfoStatus("hello", "origin"));

    assertFalse(listener.isStarted());
    assertEquals("", listener.output());
  }

  @Test
  public void startWithoutContextPrintsNothing() {
    listener.start();

    assertTrue(listener.isStarted());
    assertEquals("", listener.output());
  }

  @Test
  public void startPrintsOnlyStatusesYoungerThanTheRetrospectiveThreshold() {
    InfoStatus old = new InfoStatus("old", "origin");
    old.date = 0; // the epoch: far older than the threshold
    InfoStatus recent = new InfoStatus("recent", "origin");
    context.getStatusManager().add(old);
    context.getStatusManager().add(recent);
    listener.setContext(context);
    listener.setRetrospective(ONE_HOUR);

    listener.start();

    assertEquals(render(recent), listener.output());
  }

  @Test
  public void startWithZeroRetrospectivePrintsNoPastStatuses() {
    InfoStatus recent = new InfoStatus("recent", "origin");
    // a future timestamp is younger than even a zero threshold, so this status
    // would be printed if start() looked back at all
    recent.date = System.currentTimeMillis() + ONE_HOUR;
    context.getStatusManager().add(recent);
    listener.setContext(context);
    listener.setRetrospective(0);

    listener.start();

    assertTrue(listener.isStarted());
    assertEquals("", listener.output());
  }
}
