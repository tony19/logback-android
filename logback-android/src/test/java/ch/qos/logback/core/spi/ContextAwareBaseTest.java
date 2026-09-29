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

public class ContextAwareBaseTest {

  static final String NO_CONTEXT_WARNING = "LOGBACK: No context given for ";

  Context context = new ContextBase();
  ContextAwareBase cab = new ContextAwareBase();

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
  public void settingTheSameContextAgainIsAllowed() {
    cab.setContext(context);
    cab.setContext(context);
    assertSame(context, cab.getContext());
  }

  @Test
  public void settingAnotherContextIsRejected() {
    cab.setContext(context);
    ContextBase other = new ContextBase();

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> cab.setContext(other));

    assertEquals("Context has been already set", e.getMessage());
    assertSame(context, cab.getContext());
  }

  @Test
  public void statusManagerIsNullWithoutContext() {
    assertNull(cab.getStatusManager());
  }

  @Test
  public void statusManagerIsTheContextStatusManager() {
    cab.setContext(context);
    assertSame(context.getStatusManager(), cab.getStatusManager());
  }

  @Test
  public void everyStatusFlavorReachesTheStatusManagerWithThisAsOrigin() {
    cab.setContext(context);
    Exception infoEx = new Exception("i");
    Exception warnEx = new Exception("w");
    Exception errorEx = new Exception("e");

    cab.addInfo("info");
    cab.addInfo("info with ex", infoEx);
    cab.addWarn("warn");
    cab.addWarn("warn with ex", warnEx);
    cab.addError("error");
    cab.addError("error with ex", errorEx);

    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(6, statuses.size());
    assertStatus(statuses.get(0), Status.INFO, "info", null, cab);
    assertStatus(statuses.get(1), Status.INFO, "info with ex", infoEx, cab);
    assertStatus(statuses.get(2), Status.WARN, "warn", null, cab);
    assertStatus(statuses.get(3), Status.WARN, "warn with ex", warnEx, cab);
    assertStatus(statuses.get(4), Status.ERROR, "error", null, cab);
    assertStatus(statuses.get(5), Status.ERROR, "error with ex", errorEx, cab);
  }

  @Test
  public void declaredOriginIsTheOriginOfStatuses() {
    ContextAwareBase declaredOrigin = new ContextAwareBase();
    ContextAwareBase delegate = new ContextAwareBase(declaredOrigin);
    delegate.setContext(context);

    delegate.addInfo("hello");

    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertStatus(statuses.get(0), Status.INFO, "hello", null, declaredOrigin);
  }

  @Test
  public void withoutContextStatusesAreDroppedAndReportedOnlyOnce() throws UnsupportedEncodingException {
    cab.addInfo("first");
    cab.addError("second");
    cab.addWarn("third");

    String out = capturedOut.toString("UTF-8");
    assertTrue(out, out.startsWith(NO_CONTEXT_WARNING + cab));
    assertEquals(out, 1, out.split(NO_CONTEXT_WARNING, -1).length - 1);
    assertNull(cab.getContext());
  }

  @Test
  public void contextWithoutStatusManagerSilentlyDropsStatuses() throws UnsupportedEncodingException {
    Context contextWithoutStatusManager = mock(Context.class);
    cab.setContext(contextWithoutStatusManager);

    cab.addError("dropped");

    verify(contextWithoutStatusManager).getStatusManager();
    assertEquals("", capturedOut.toString("UTF-8"));
  }

  private static void assertStatus(Status status, int level, String message, Throwable throwable,
                                   Object origin) {
    assertEquals(level, status.getLevel());
    assertEquals(message, status.getMessage());
    assertSame(throwable, status.getThrowable());
    assertSame(origin, status.getOrigin());
  }
}
