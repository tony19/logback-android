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

import static junit.framework.Assert.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.helpers.ThrowableToStringArray;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;

public class StatusPrinterTest {

  ByteArrayOutputStream outputStream;
  PrintStream ps;
  
  @Before
  public void setUp() throws Exception {
    outputStream = new ByteArrayOutputStream();
    ps = new PrintStream(outputStream);
    StatusPrinter.setPrintStream(ps);
  }

  @After
  public void tearDown() throws Exception {
    StatusPrinter.setPrintStream(System.out);
    ps = null;
    outputStream = null;
  }
  
  @Test
  public void testBasic() {
    Context context = new ContextBase();
    context.getStatusManager().add(new InfoStatus("test", this));
    StatusPrinter.print(context);
    String result = outputStream.toString();
    assertTrue(result.contains("|-INFO in "+this.getClass().getName()));
  }

  @Test
  public void testNested() {
    Status s0 = new ErrorStatus("test0", this);
    Status s1 = new InfoStatus("test1", this);
    Status s11 = new InfoStatus("test11", this);
    Status s12 = new InfoStatus("test12", this);
    s1.add(s11);
    s1.add(s12);
    
    Status s2 = new InfoStatus("test2", this);
    Status s21 = new InfoStatus("test21", this);
    Status s211 = new WarnStatus("test211", this);
    
    Status s22 = new InfoStatus("test22", this);
    s2.add(s21);
    s2.add(s22);
    s21.add(s211);
    
    
    Context context = new ContextBase();
    context.getStatusManager().add(s0);
    context.getStatusManager().add(s1);
    context.getStatusManager().add(s2);

    StatusPrinter.print(context);
    String result = outputStream.toString();
    assertTrue(result.contains("+ INFO in "+this.getClass().getName()));
    assertTrue(result.contains("+ WARN in "+this.getClass().getName()));
    assertTrue(result.contains("    |-WARN in "+this.getClass().getName()));
  }

  @Test
  public void testWithException() {
    Status s0 = new ErrorStatus("test0", this);
    Status s1 = new InfoStatus("test1", this, new Exception("testEx"));
    Status s11 = new InfoStatus("test11", this);
    Status s12 = new InfoStatus("test12", this);
    s1.add(s11);
    s1.add(s12);
    
    Status s2 = new InfoStatus("test2", this);
    Status s21 = new InfoStatus("test21", this);
    Status s211 = new WarnStatus("test211", this);
    
    Status s22 = new InfoStatus("test22", this);
    s2.add(s21);
    s2.add(s22);
    s21.add(s211);
    
    Context context = new ContextBase();
    context.getStatusManager().add(s0);
    context.getStatusManager().add(s1);
    context.getStatusManager().add(s2);
    StatusPrinter.print(context);  
    String result = outputStream.toString();
    assertTrue(result.contains("|-ERROR in "+this.getClass().getName()));
    assertTrue(result.contains("+ INFO in "+this.getClass().getName()));
    assertTrue(result.contains("ch.qos.logback.core.util.StatusPrinterTest.testWithException"));
  }

  static final String ORIGIN = "statusPrinterTest";
  static final String LS = CoreConstants.LINE_SEPARATOR;
  static final String NO_STATUS_MANAGER_WARNING = "WARN: Context named \"noStatusManager\" has no status manager" + LS;

  /** A context whose status manager is missing. */
  static Context contextWithoutStatusManager() {
    ContextBase context = new ContextBase() {
      @Override
      public StatusManager getStatusManager() {
        return null;
      }
    };
    context.setName("noStatusManager");
    return context;
  }

  @Test
  public void isInstantiable() {
    // the class only has static members, but its implicit constructor is public
    assertNotNull(new StatusPrinter());
  }

  @Test
  public void printInCaseOfErrorsOrWarningsRejectsNullContext() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> StatusPrinter.printInCaseOfErrorsOrWarnings(null));
    assertEquals("Context argument cannot be null", e.getMessage());
  }

  @Test
  public void printInCaseOfErrorsOrWarningsWarnsAboutMissingStatusManager() {
    StatusPrinter.printInCaseOfErrorsOrWarnings(contextWithoutStatusManager());
    assertEquals(NO_STATUS_MANAGER_WARNING, outputStream.toString());
  }

  @Test
  public void printInCaseOfErrorsOrWarningsPrintsOnlyIfThereAreWarnings() {
    Context context = new ContextBase();
    context.getStatusManager().add(new InfoStatus("info0", ORIGIN));
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    assertEquals("", outputStream.toString());

    context.getStatusManager().add(new WarnStatus("warn0", ORIGIN));
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    String result = outputStream.toString();
    assertTrue(result.contains("|-INFO in " + ORIGIN + " - info0"));
    assertTrue(result.contains("|-WARN in " + ORIGIN + " - warn0"));
  }

  @Test
  public void printIfErrorsOccuredRejectsNullContext() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> StatusPrinter.printIfErrorsOccured(null));
    assertEquals("Context argument cannot be null", e.getMessage());
  }

  @Test
  public void printIfErrorsOccuredWarnsAboutMissingStatusManager() {
    StatusPrinter.printIfErrorsOccured(contextWithoutStatusManager());
    assertEquals(NO_STATUS_MANAGER_WARNING, outputStream.toString());
  }

  @Test
  public void printIfErrorsOccuredPrintsOnlyIfThereAreErrors() {
    Context context = new ContextBase();
    context.getStatusManager().add(new WarnStatus("warn0", ORIGIN));
    StatusPrinter.printIfErrorsOccured(context);
    assertEquals("", outputStream.toString());

    context.getStatusManager().add(new ErrorStatus("error0", ORIGIN));
    StatusPrinter.printIfErrorsOccured(context);
    String result = outputStream.toString();
    assertTrue(result.contains("|-WARN in " + ORIGIN + " - warn0"));
    assertTrue(result.contains("|-ERROR in " + ORIGIN + " - error0"));
  }

  @Test
  public void printRejectsNullContext() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> StatusPrinter.print((Context) null));
    assertEquals("Context argument cannot be null", e.getMessage());
  }

  @Test
  public void printWarnsAboutMissingStatusManager() {
    StatusPrinter.print(contextWithoutStatusManager());
    assertEquals(NO_STATUS_MANAGER_WARNING, outputStream.toString());
  }

  @Test
  public void printStatusList() {
    List<Status> statusList = new ArrayList<Status>();
    statusList.add(new InfoStatus("info0", ORIGIN));
    statusList.add(new ErrorStatus("error0", ORIGIN));

    StatusPrinter.print(statusList);

    String[] lines = outputStream.toString().split(LS);
    assertEquals(2, lines.length);
    assertTrue(lines[0], lines[0].endsWith("|-INFO in " + ORIGIN + " - info0"));
    assertTrue(lines[1], lines[1].endsWith("|-ERROR in " + ORIGIN + " - error0"));
  }

  @Test
  public void printNullStatusListPrintsAnEmptyLine() {
    StatusPrinter.print((List<Status>) null);
    assertEquals(LS, outputStream.toString());
  }

  @Test
  public void buildStrOmitsTheDateWithoutDateFormatter() {
    CachingDateFormatter original = StatusPrinter.cachingDateFormat;
    StatusPrinter.cachingDateFormat = null;
    try {
      StringBuilder sb = new StringBuilder();
      StatusPrinter.buildStr(sb, "", new InfoStatus("info0", ORIGIN));
      assertEquals("|-INFO in " + ORIGIN + " - info0" + LS, sb.toString());
    } finally {
      StatusPrinter.cachingDateFormat = original;
    }
  }

  @Test
  public void buildStrPrefixesEachThrowableLineByItsKind() {
    Throwable t = new Exception("boom");
    CachingDateFormatter original = StatusPrinter.cachingDateFormat;
    StatusPrinter.cachingDateFormat = null;
    try (MockedStatic<ThrowableToStringArray> converter = mockStatic(ThrowableToStringArray.class)) {
      converter.when(() -> ThrowableToStringArray.convert(t)).thenReturn(new String[] {
          "java.lang.Exception: boom",
          "Caused by: java.lang.IllegalStateException: cause",
          "12 common frames omitted"
      });
      StringBuilder sb = new StringBuilder();
      StatusPrinter.buildStr(sb, "", new ErrorStatus("error0", ORIGIN, t));

      assertEquals("|-ERROR in " + ORIGIN + " - error0 java.lang.Exception: boom" + LS
          + "\tat java.lang.Exception: boom" + LS
          + "Caused by: java.lang.IllegalStateException: cause" + LS
          + "\t... 12 common frames omitted" + LS, sb.toString());
    } finally {
      StatusPrinter.cachingDateFormat = original;
    }
  }

}
