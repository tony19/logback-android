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
package ch.qos.logback.core.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.WarnStatus;

public class DynamicConverterTest {

  private final Context context = new ContextBase();
  private final ConverterHello converter = new ConverterHello();

  @Before
  public void setUp() {
    converter.setContext(context);
  }

  private Status onlyStatus() {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    return statuses.get(0);
  }

  private void assertOnlyStatus(Class<? extends Status> type, int level, String msg, Throwable ex) {
    Status s = onlyStatus();
    assertSame(type, s.getClass());
    assertEquals(level, s.getLevel());
    assertEquals(msg, s.getMessage());
    assertSame(ex, s.getThrowable());
    assertSame(converter, s.getOrigin());
  }

  @Test
  public void getContextReturnsTheContextSet() {
    assertSame(context, converter.getContext());
  }

  @Test
  public void startAndStopToggleIsStarted() {
    assertFalse(converter.isStarted());
    converter.start();
    assertTrue(converter.isStarted());
    converter.stop();
    assertFalse(converter.isStarted());
  }

  @Test
  public void firstOptionIsNullWithoutOptionList() {
    assertNull(converter.getFirstOption());
    assertNull(converter.getOptionList());
  }

  @Test
  public void firstOptionIsNullForEmptyOptionList() {
    converter.setOptionList(Collections.<String>emptyList());
    assertNull(converter.getFirstOption());
  }

  @Test
  public void firstOptionIsTheFirstElementOfTheOptionList() {
    List<String> options = Arrays.asList("first", "second");
    converter.setOptionList(options);
    assertEquals("first", converter.getFirstOption());
    assertSame(options, converter.getOptionList());
  }

  @Test
  public void addStatusForwardsTheStatusToTheContext() {
    Status status = new InfoStatus("hello", this);
    converter.addStatus(status);
    assertSame(status, onlyStatus());
  }

  @Test
  public void addInfoRecordsAnInfoStatus() {
    converter.addInfo("info msg");
    assertOnlyStatus(InfoStatus.class, Status.INFO, "info msg", null);
  }

  @Test
  public void addInfoWithThrowableRecordsTheThrowable() {
    Exception ex = new Exception("boom");
    converter.addInfo("info msg", ex);
    assertOnlyStatus(InfoStatus.class, Status.INFO, "info msg", ex);
  }

  @Test
  public void addWarnRecordsAWarnStatus() {
    converter.addWarn("warn msg");
    assertOnlyStatus(WarnStatus.class, Status.WARN, "warn msg", null);
  }

  @Test
  public void addWarnWithThrowableRecordsTheThrowable() {
    Exception ex = new Exception("boom");
    converter.addWarn("warn msg", ex);
    assertOnlyStatus(WarnStatus.class, Status.WARN, "warn msg", ex);
  }

  @Test
  public void addErrorRecordsAnErrorStatus() {
    converter.addError("error msg");
    assertOnlyStatus(ErrorStatus.class, Status.ERROR, "error msg", null);
  }

  @Test
  public void addErrorWithThrowableRecordsTheThrowable() {
    Exception ex = new Exception("boom");
    converter.addError("error msg", ex);
    assertOnlyStatus(ErrorStatus.class, Status.ERROR, "error msg", ex);
  }
}
