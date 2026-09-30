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
package ch.qos.logback.classic.spi;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CallerDataTest  {

  private static final String FQCN = "com.acme.logging.MyLogger";
  private static final List<String> FRAMEWORK_PACKAGES = Arrays.asList("com.acme.wrapper", "org.example.facade");

  @Test
  public void testBasic() {
    Throwable t = new Throwable();
    StackTraceElement[] steArray = t.getStackTrace();
    
    StackTraceElement[] cda = CallerData.extract(t, CallerDataTest.class.getName(), 50, null);
    assertNotNull(cda);
    assertTrue(cda.length > 0);
    assertEquals(steArray.length - 1, cda.length);
  }
  
  /**
   * This test verifies that in case caller data cannot be
   * extracted, CallerData.extract does not throw an exception
   *
   */
  @Test
  public void testDeferredProcessing() {
    StackTraceElement[] cda = CallerData.extract(new Throwable(), "com.inexistent.foo", 10, null);
    assertNotNull(cda);
    assertEquals(0, cda.length);
  }

  @Test
  public void extractReturnsNullWithoutThrowable() {
    assertNull(CallerData.extract(null, FQCN, 10, FRAMEWORK_PACKAGES));
  }

  @Test
  public void callerIsTheFirstFrameAfterAllFrameworkFrames() {
    StackTraceElement caller = ste("com.app.Service");
    StackTraceElement main = ste("com.app.Main");
    Throwable t = throwableWithFrames(
        ste(FQCN),
        ste("org.slf4j.LoggerFactoryHelper"),     // starts with org.slf4j.Logger
        ste("org.apache.log4j.Category"),         // log4j-over-slf4j
        ste("com.acme.wrapper.LogWrapper"),       // in the framework package list
        ste("org.example.facade.sub.LogFacade"),  // sub-package of a listed package
        caller,
        main);

    StackTraceElement[] cda = CallerData.extract(t, FQCN, 10, FRAMEWORK_PACKAGES);

    assertArrayEquals(new StackTraceElement[] {caller, main}, cda);
  }

  @Test
  public void callerDataIsTruncatedToMaxDepth() {
    StackTraceElement caller = ste("com.app.Service");
    Throwable t = throwableWithFrames(ste(FQCN), caller, ste("com.app.Main"), ste("com.app.Launcher"));

    StackTraceElement[] cda = CallerData.extract(t, FQCN, 1, FRAMEWORK_PACKAGES);

    assertArrayEquals(new StackTraceElement[] {caller}, cda);
  }

  @Test
  public void frameworkFramesBelowTheCallerDoNotMoveTheCaller() {
    StackTraceElement caller = ste("com.app.Service");
    StackTraceElement wrapperBelow = ste("com.acme.wrapper.Dispatcher");
    Throwable t = throwableWithFrames(ste(FQCN), caller, wrapperBelow);

    StackTraceElement[] cda = CallerData.extract(t, FQCN, 10, FRAMEWORK_PACKAGES);

    assertArrayEquals(new StackTraceElement[] {caller, wrapperBelow}, cda);
  }

  @Test
  public void onlyClassesOfListedPackagesAreInFrameworkSpace() {
    assertTrue(CallerData.isInFrameworkSpace("org.example.facade.X", FQCN, FRAMEWORK_PACKAGES));
    assertFalse(CallerData.isInFrameworkSpace("com.app.Service", FQCN, FRAMEWORK_PACKAGES));
    assertFalse(CallerData.isInFrameworkSpace("com.app.Service", FQCN, Collections.<String>emptyList()));
    assertFalse(CallerData.isInFrameworkSpace("com.app.Service", FQCN, null));
  }

  @Test
  public void naInstanceHasUnavailableFields() {
    StackTraceElement na = CallerData.naInstance();

    assertEquals(CallerData.NA, na.getClassName());
    assertEquals(CallerData.NA, na.getMethodName());
    assertEquals(CallerData.NA, na.getFileName());
    assertEquals(CallerData.LINE_NA, na.getLineNumber());
  }

  @Test
  public void publicConstructorIsAvailable() {
    // the class only has static members, but its implicit public constructor is API
    assertNotNull(new CallerData());
  }

  private static StackTraceElement ste(String className) {
    return new StackTraceElement(className, "method", "File.java", 42);
  }

  private static Throwable throwableWithFrames(StackTraceElement... frames) {
    Throwable t = new Throwable();
    t.setStackTrace(frames);
    return t;
  }
}
