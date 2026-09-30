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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;

public class ContextAwareImplTest {

  static final String NO_CONTEXT_WARNING = "LOGBACK: No context given for ";

  Context context = new ContextBase();
  Object origin = "the origin";

  PrintStream originalOut;
  ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

  @Before
  public void setUp() throws UnsupportedEncodingException {
    originalOut = System.out;
    System.setOut(new PrintStream(capturedOut, true, "UTF-8"));
  }

  @After
  public void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  public void contextCanBeSetWhenNoneWasGiven() {
    ContextAwareImpl cai = new ContextAwareImpl(null, origin);
    assertNull(cai.getContext());

    cai.setContext(context);

    assertSame(context, cai.getContext());
  }

  @Test
  public void settingTheSameContextAgainIsAllowed() {
    ContextAwareImpl cai = new ContextAwareImpl(context, origin);
    cai.setContext(context);
    assertSame(context, cai.getContext());
  }

  @Test
  public void settingAnotherContextIsRejected() {
    ContextAwareImpl cai = new ContextAwareImpl(context, origin);
    ContextBase other = new ContextBase();

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> cai.setContext(other));

    assertEquals("Context has been already set", e.getMessage());
    assertSame(context, cai.getContext());
  }

  @Test
  public void statusManagerIsNullWithoutContext() {
    assertNull(new ContextAwareImpl(null, origin).getStatusManager());
  }

  @Test
  public void statusManagerIsTheContextStatusManager() {
    assertSame(context.getStatusManager(), new ContextAwareImpl(context, origin).getStatusManager());
  }

  @Test
  public void everyStatusFlavorReachesTheStatusManagerWithTheGivenOrigin() {
    ContextAwareImpl cai = new ContextAwareImpl(context, origin);

    assertEveryStatusFlavorIsAddedWithOrigin(cai, origin);
  }

  @Test
  public void overriddenOriginIsTheOriginOfEveryStatusFlavor() {
    final Object overriddenOrigin = new Object();
    ContextAwareImpl cai = new ContextAwareImpl(context, origin) {
      @Override
      protected Object getOrigin() {
        return overriddenOrigin;
      }
    };

    assertEveryStatusFlavorIsAddedWithOrigin(cai, overriddenOrigin);
  }

  @Test
  public void withoutContextStatusesAreDroppedAndReportedOnlyOnce() throws UnsupportedEncodingException {
    ContextAwareImpl cai = new ContextAwareImpl(null, origin);

    cai.addInfo("first");
    cai.addError("second");
    cai.addWarn("third");

    String out = capturedOut.toString("UTF-8");
    assertTrue(out, out.startsWith(NO_CONTEXT_WARNING + origin));
    assertEquals(out, 1, out.split(NO_CONTEXT_WARNING, -1).length - 1);
  }

  @Test
  public void contextWithoutStatusManagerSilentlyDropsStatuses() throws UnsupportedEncodingException {
    Context contextWithoutStatusManager = mock(Context.class);
    ContextAwareImpl cai = new ContextAwareImpl(contextWithoutStatusManager, origin);

    cai.addError("dropped");

    verify(contextWithoutStatusManager).getStatusManager();
    assertEquals("", capturedOut.toString("UTF-8"));
  }

  private void assertEveryStatusFlavorIsAddedWithOrigin(ContextAwareImpl cai, Object expectedOrigin) {
    Exception infoEx = new Exception("i");
    Exception warnEx = new Exception("w");
    Exception errorEx = new Exception("e");

    cai.addInfo("info");
    cai.addInfo("info with ex", infoEx);
    cai.addWarn("warn");
    cai.addWarn("warn with ex", warnEx);
    cai.addError("error");
    cai.addError("error with ex", errorEx);

    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(6, statuses.size());
    assertStatus(statuses.get(0), Status.INFO, "info", null, expectedOrigin);
    assertStatus(statuses.get(1), Status.INFO, "info with ex", infoEx, expectedOrigin);
    assertStatus(statuses.get(2), Status.WARN, "warn", null, expectedOrigin);
    assertStatus(statuses.get(3), Status.WARN, "warn with ex", warnEx, expectedOrigin);
    assertStatus(statuses.get(4), Status.ERROR, "error", null, expectedOrigin);
    assertStatus(statuses.get(5), Status.ERROR, "error with ex", errorEx, expectedOrigin);
  }

  private static void assertStatus(Status status, int level, String message, Throwable throwable,
                                   Object expectedOrigin) {
    assertEquals(level, status.getLevel());
    assertEquals(message, status.getMessage());
    assertSame(throwable, status.getThrowable());
    assertSame(expectedOrigin, status.getOrigin());
  }
}
