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

import java.io.IOException;
import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class StatusUtilTest {

  Context context = new ContextBase();
  StatusUtil statusUtil = new StatusUtil(context);

  @Test
  public void emptyStatusListShouldResultInNotFound() {
    assertEquals(-1, statusUtil.timeOfLastReset());
  }

  @Test
  public void withoutResetsStatusUtilShouldReturnNotFound() {
    context.getStatusManager().add(new InfoStatus("test", this));
    assertEquals(-1, statusUtil.timeOfLastReset());
  }

  @Test
  public void statusListShouldReturnLastResetTime() {
    context.getStatusManager().add(new InfoStatus("test", this));
    long resetTime = System.currentTimeMillis();
    context.getStatusManager().add(new InfoStatus(CoreConstants.RESET_MSG_PREFIX, this));
    context.getStatusManager().add(new InfoStatus("bla", this));
    assertTrue(resetTime <= statusUtil.timeOfLastReset());
  }

  @Test
  public void timeOfLastResetIsNotFoundWhenStatusManagerReturnsNoList() {
    StatusManager sm = mock(StatusManager.class);
    when(sm.getCopyOfStatusList()).thenReturn(null);

    assertEquals(-1, new StatusUtil(sm).timeOfLastReset());
  }

  @Test
  public void contextWithoutStatusManagerHasNoStatusListener() {
    Context contextWithoutSm = mock(Context.class);
    when(contextWithoutSm.getStatusManager()).thenReturn(null);

    assertFalse(StatusUtil.contextHasStatusListener(contextWithoutSm));
  }

  @Test
  public void statusManagerReturningNoListenerListMeansNoStatusListener() {
    StatusManager sm = mock(StatusManager.class);
    when(sm.getCopyOfStatusListenerList()).thenReturn(null);
    Context mockContext = mock(Context.class);
    when(mockContext.getStatusManager()).thenReturn(sm);

    assertFalse(StatusUtil.contextHasStatusListener(mockContext));
  }

  @Test
  public void contextWithoutRegisteredListenersHasNoStatusListener() {
    assertFalse(StatusUtil.contextHasStatusListener(context));
  }

  @Test
  public void contextWithRegisteredListenerHasStatusListener() {
    context.getStatusManager().add(new NopStatusListener());

    assertTrue(StatusUtil.contextHasStatusListener(context));
  }

  @Test
  public void addStatusAddsToTheStatusManager() {
    InfoStatus status = new InfoStatus("hello", this);

    statusUtil.addStatus(status);

    List<Status> list = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, list.size());
    assertSame(status, list.get(0));
  }

  @Test
  public void addStatusWithoutStatusManagerIsIgnoredInsteadOfFailing() {
    StatusUtil withoutSm = new StatusUtil((StatusManager) null);

    // must not throw a NullPointerException
    withoutSm.addStatus(new InfoStatus("hello", this));
    withoutSm.addWarn(this, "warn");
  }

  @Test
  public void addInfoAddsInfoStatusWithCallerAsOrigin() {
    statusUtil.addInfo("caller", "info message");

    Status status = onlyStatus();
    assertTrue(status instanceof InfoStatus);
    assertEquals(Status.INFO, status.getLevel());
    assertEquals("info message", status.getMessage());
    assertEquals("caller", status.getOrigin());
    assertNull(status.getThrowable());
  }

  @Test
  public void addWarnAddsWarnStatusWithCallerAsOrigin() {
    statusUtil.addWarn("caller", "warn message");

    Status status = onlyStatus();
    assertTrue(status instanceof WarnStatus);
    assertEquals(Status.WARN, status.getLevel());
    assertEquals("warn message", status.getMessage());
    assertEquals("caller", status.getOrigin());
    assertNull(status.getThrowable());
  }

  @Test
  public void addErrorAddsErrorStatusWithCallerAsOriginAndThrowable() {
    Exception cause = new Exception("cause");

    statusUtil.addError("caller", "error message", cause);

    Status status = onlyStatus();
    assertTrue(status instanceof ErrorStatus);
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals("error message", status.getMessage());
    assertEquals("caller", status.getOrigin());
    assertSame(cause, status.getThrowable());
  }

  @Test
  public void onlyInfoStatusesAreWarningAndErrorFree() {
    statusUtil.addInfo(this, "info");

    assertTrue(statusUtil.isErrorFree(0));
    assertTrue(statusUtil.isWarningOrErrorFree(0));
  }

  @Test
  public void warningIsErrorFreeButNotWarningFree() {
    statusUtil.addInfo(this, "info");
    statusUtil.addWarn(this, "warn");

    assertTrue(statusUtil.isErrorFree(0));
    assertFalse(statusUtil.isWarningOrErrorFree(0));
  }

  @Test
  public void errorIsNeitherErrorFreeNorWarningFree() {
    statusUtil.addError(this, "error", null);

    assertFalse(statusUtil.isErrorFree(0));
    assertFalse(statusUtil.isWarningOrErrorFree(0));
  }

  @Test
  public void containsExceptionFindsExceptionTypeInCauseChain() {
    statusUtil.addInfo(this, "no throwable");
    statusUtil.addError(this, "wrapped", new IOException("outer", new IllegalStateException("inner")));

    assertTrue(statusUtil.containsException(IOException.class));
    assertTrue(statusUtil.containsException(IllegalStateException.class));
  }

  @Test
  public void containsExceptionIsFalseWhenNoStatusCarriesThatType() {
    statusUtil.addInfo(this, "no throwable");
    statusUtil.addError(this, "wrapped", new IOException("outer", new IllegalStateException("inner")));

    assertFalse(statusUtil.containsException(ArithmeticException.class));
  }

  private Status onlyStatus() {
    List<Status> list = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, list.size());
    return list.get(0);
  }

}
