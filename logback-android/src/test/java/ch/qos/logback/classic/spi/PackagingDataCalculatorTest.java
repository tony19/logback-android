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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.classic.util.TestHelper;

public class PackagingDataCalculatorTest {

  private ClassLoader originalContextClassLoader;

  @Before
  public void saveContextClassLoader() {
    originalContextClassLoader = Thread.currentThread().getContextClassLoader();
  }

  @After
  public void restoreContextClassLoader() {
    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
  }

  public void verify(ThrowableProxy tp) {
    for (StackTraceElementProxy step : tp.getStackTraceElementProxyArray()) {
      if (step != null) {
        assertNotNull(step.getClassPackagingData());
      }
    }
  }

  @Test
  public void smoke() throws Exception {
    Throwable t = new Throwable("x");
    ThrowableProxy tp = new ThrowableProxy(t);
    PackagingDataCalculator pdc = tp.getPackagingDataCalculator();
    pdc.calculate(tp);
    verify(tp);
    tp.fullDump();
  }

  @Test
  public void nested() throws Exception {
    Throwable t = TestHelper.makeNestedException(3);
    ThrowableProxy tp = new ThrowableProxy(t);
    PackagingDataCalculator pdc = tp.getPackagingDataCalculator();
    pdc.calculate(tp);
    verify(tp);
  }

  public void doCalculateClassPackagingData(
      boolean withClassPackagingCalculation) {
    try {
      throw new Exception("testing");
    } catch (Throwable e) {
      ThrowableProxy tp = new ThrowableProxy(e);
      if (withClassPackagingCalculation) {
        PackagingDataCalculator pdc = tp.getPackagingDataCalculator();
        pdc.calculate(tp);
      }
    }
  }

  double loop(int len, boolean withClassPackagingCalculation) {
    long start = System.nanoTime();
    for (int i = 0; i < len; i++) {
      doCalculateClassPackagingData(withClassPackagingCalculation);
    }
    return (1.0 * System.nanoTime() - start) / len / 1000;
  }

  @Ignore
  @Test
  public void perfTest() {
    int len = 1000;
    loop(len, false);
    loop(len, true);

    double d0 = loop(len, false);
    System.out.println("without packaging info " + d0 + " microseconds");

    double d1 = loop(len, true);
    System.out.println("with    packaging info " + d1 + " microseconds");

    // be more lenient with other JDKs, esp for logback-android,
    // which computes packaging info by STEP (slow)
    int slackFactor = 15;

    assertTrue("computing class packaging data (" + d1
        + ") should have been less than " + slackFactor
        + " times the time it takes to process an exception "
        + (d0 * slackFactor), d0 * slackFactor > d1);

  }

  private ClassLoader makeBogusClassLoader() throws MalformedURLException {
    ClassLoader currentClassLoader = this.getClass().getClassLoader();
    return new BogusClassLoader(new URL[] {},
        currentClassLoader);
  }

  @Test
  // Test http://jira.qos.ch/browse/LBCLASSIC-125
  public void noClassDefFoundError_LBCLASSIC_125Test()
      throws MalformedURLException {
    ClassLoader cl = (URLClassLoader) makeBogusClassLoader();
    Thread.currentThread().setContextClassLoader(cl);
    Throwable t = new Throwable("x");
    ThrowableProxy tp = new ThrowableProxy(t);
    StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
    StackTraceElement bogusSTE = new StackTraceElement("com.Bogus", "myMethod",
        "myFile", 12);
    stepArray[0] = new StackTraceElementProxy(bogusSTE);
    PackagingDataCalculator pdc = tp.getPackagingDataCalculator();
    // NoClassDefFoundError should be caught
    pdc.calculate(tp);
    assertNotAvailable(stepArray[0].getClassPackagingData());
  }

  @Test
  public void suppressedThrowablesGetPackagingData() {
    Exception main = new Exception("main");
    main.addSuppressed(new Exception("suppressed"));
    ThrowableProxy tp = new ThrowableProxy(main);

    new PackagingDataCalculator().calculate(tp);

    IThrowableProxy suppressed = tp.getSuppressed()[0];
    assertTrue(suppressed.getStackTraceElementProxyArray().length > 0);
    for (StackTraceElementProxy step : suppressed.getStackTraceElementProxyArray()) {
      assertNotNull(step.getClassPackagingData());
    }
  }

  @Test
  public void proxyWithoutSuppressedArrayIsSupported() {
    StackTraceElementProxy step = step(Test.class.getName());
    DummyThrowableProxy tp = proxyOf(step);
    tp.setSuppressed(null);

    new PackagingDataCalculator().calculate(tp);

    assertPackagingDataOf(Test.class, step.getClassPackagingData());
  }

  @Test
  public void framesOfTheSameClassShareCachedPackagingData() {
    StackTraceElementProxy first = step(Test.class.getName());
    StackTraceElementProxy second = step(Test.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(first, second));

    assertSame(first.getClassPackagingData(), second.getClassPackagingData());
  }

  @Test
  public void classIsResolvedWithClassForNameWhenThereIsNoContextClassLoader() throws Exception {
    Thread.currentThread().setContextClassLoader(null);
    StackTraceElementProxy step = step(Test.class.getName());

    String err = calculateCapturingStdErr(proxyOf(step));

    assertPackagingDataOf(Test.class, step.getClassPackagingData());
    // a missing class loader is expected, not an error to report
    assertEquals("", err);
  }

  @Test
  public void classUnknownToTheContextClassLoaderIsResolvedWithClassForName() throws IOException {
    URLClassLoader bootstrapOnly = new URLClassLoader(new URL[0], null);
    try {
      Thread.currentThread().setContextClassLoader(bootstrapOnly);
      StackTraceElementProxy step = step(Test.class.getName());

      String err = calculateCapturingStdErr(proxyOf(step));

      assertPackagingDataOf(Test.class, step.getClassPackagingData());
      // the ClassNotFoundException of the context class loader is expected, not reported
      assertEquals("", err);
    } finally {
      bootstrapOnly.close();
    }
  }

  @Test
  public void failingContextClassLoaderIsReportedAndClassForNameIsUsed() throws Exception {
    Thread.currentThread().setContextClassLoader(new ClassLoader(null) {
      @Override
      public Class<?> loadClass(String name) {
        throw new IllegalStateException("unexpected loader failure");
      }
    });
    StackTraceElementProxy step = step(Test.class.getName());

    String err = calculateCapturingStdErr(proxyOf(step));

    assertTrue(err, err.contains("java.lang.IllegalStateException: unexpected loader failure"));
    assertPackagingDataOf(Test.class, step.getClassPackagingData());
  }

  @Test
  public void arrayClassHasNoPackageAndThusNoVersion() {
    // the application class loader cannot load an array class, Class.forName can
    StackTraceElementProxy step = step(String[].class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    ClassPackagingData cpd = step.getClassPackagingData();
    assertEquals("na", cpd.getVersion());
    assertEquals("na", cpd.getCodeLocation());
    assertFalse(cpd.isExact());
  }

  @Test
  public void classWhoseInitializationFailedHasNoPackagingData() {
    Thread.currentThread().setContextClassLoader(null);
    String className = PackagingDataCalculatorFailingInit.class.getName();
    try {
      Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    } catch (LinkageError expected) {
      // ExceptionInInitializerError the first time, NoClassDefFoundError afterwards
    }
    StackTraceElementProxy step = step(className);

    // Class.forName now throws NoClassDefFoundError, which must be swallowed
    new PackagingDataCalculator().calculate(proxyOf(step));

    assertNotAvailable(step.getClassPackagingData());
  }

  @Test
  public void unexpectedClassForNameFailureIsReportedAndSwallowed() throws Exception {
    Thread.currentThread().setContextClassLoader(null);
    // a mocked frame without class name makes Class.forName throw a NullPointerException
    StackTraceElement nameless = mock(StackTraceElement.class);
    doReturn(null).when(nameless).getClassName();
    StackTraceElementProxy step = new StackTraceElementProxy(nameless);

    String err = calculateCapturingStdErr(proxyOf(step));

    assertTrue(err, err.contains("java.lang.NullPointerException"));
    assertNotAvailable(step.getClassPackagingData());
  }

  @Test
  public void codeSourceWithoutLocationHasNoCodeLocation() throws Exception {
    useSubjectLoader(new CodeSource(null, (Certificate[]) null));
    StackTraceElementProxy step = step(PackagingDataCalculatorSubject.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    assertEquals("na", step.getClassPackagingData().getCodeLocation());
  }

  @Test
  public void jarNameIsExtractedFromWindowsStyleLocation() throws Exception {
    useSubjectLoader(codeSourceAt("file:C:\\java\\lib\\subject-1.0.jar"));
    StackTraceElementProxy step = step(PackagingDataCalculatorSubject.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    assertEquals("subject-1.0.jar", step.getClassPackagingData().getCodeLocation());
  }

  @Test
  public void jarNameIsExtractedFromUrlStyleLocation() throws Exception {
    useSubjectLoader(codeSourceAt("file:/opt/lib/subject-1.0.jar"));
    StackTraceElementProxy step = step(PackagingDataCalculatorSubject.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    assertEquals("subject-1.0.jar", step.getClassPackagingData().getCodeLocation());
  }

  @Test
  public void folderNameIsExtractedFromFolderLocation() throws Exception {
    useSubjectLoader(codeSourceAt("file:/opt/app/classes/"));
    StackTraceElementProxy step = step(PackagingDataCalculatorSubject.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    assertEquals("classes/", step.getClassPackagingData().getCodeLocation());
  }

  @Test
  public void leadingSeparatorAloneDoesNotDelimitACodeLocation() throws Exception {
    // a separator at index 0 is not taken as the start of a file name
    useSubjectLoader(codeSourceAt("/subject-1.0.jar"));
    StackTraceElementProxy step = step(PackagingDataCalculatorSubject.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    assertNull(step.getClassPackagingData().getCodeLocation());
  }

  @Test
  public void classWithoutImplementationVersionHasNaVersion() throws Exception {
    SubjectClassLoader loader = useSubjectLoader(codeSourceAt("file:/opt/lib/subject-1.0.jar"));
    Class<?> subject = loader.loadClass(PackagingDataCalculatorSubject.class.getName());
    // precondition: the subject's package is defined without manifest attributes
    assertNotNull(subject.getPackage());
    assertNull(subject.getPackage().getImplementationVersion());
    StackTraceElementProxy step = step(PackagingDataCalculatorSubject.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    ClassPackagingData cpd = step.getClassPackagingData();
    assertEquals("na", cpd.getVersion());
    assertEquals("subject-1.0.jar", cpd.getCodeLocation());
    assertFalse(cpd.isExact());
  }

  @Test
  public void failureWhileReadingTheCodeLocationIsSwallowed() throws Exception {
    LocationHandler handler = new LocationHandler("file:/opt/lib/subject-1.0.jar");
    SubjectClassLoader loader = useSubjectLoader(new CodeSource(handler.url(), (Certificate[]) null));
    loader.loadClass(PackagingDataCalculatorSubject.class.getName());
    handler.failFromNowOn();
    StackTraceElementProxy step = step(PackagingDataCalculatorSubject.class.getName());

    new PackagingDataCalculator().calculate(proxyOf(step));

    assertEquals("na", step.getClassPackagingData().getCodeLocation());
  }

  @Test
  public void givenClassLoaderIsTriedBeforeTheContextClassLoader() throws Exception {
    // populateFrames() only ever passes a null class loader (the port has no
    // Reflection.getCallerClass()), so reach the non-null case reflectively
    SubjectClassLoader loader = new SubjectClassLoader(new CodeSource(null, (Certificate[]) null));
    Method bestEffortLoadClass = PackagingDataCalculator.class.getDeclaredMethod("bestEffortLoadClass",
        ClassLoader.class, String.class);
    bestEffortLoadClass.setAccessible(true);

    Class<?> type = (Class<?>) bestEffortLoadClass.invoke(new PackagingDataCalculator(), loader,
        PackagingDataCalculatorSubject.class.getName());

    assertSame(loader, type.getClassLoader());
    assertNotSame(PackagingDataCalculatorSubject.class, type);
  }

  private static StackTraceElementProxy step(String className) {
    return new StackTraceElementProxy(new StackTraceElement(className, "method", "File.java", 1));
  }

  private static DummyThrowableProxy proxyOf(StackTraceElementProxy... steps) {
    DummyThrowableProxy tp = new DummyThrowableProxy();
    tp.setClassName("java.lang.Exception");
    tp.setStackTraceElementProxyArray(steps);
    tp.setSuppressed(new IThrowableProxy[0]);
    return tp;
  }

  /**
   * Runs the calculation and returns what it wrote to System.err. Only the output of
   * this thread is captured; other threads' output still goes to the original stream.
   */
  private static String calculateCapturingStdErr(IThrowableProxy tp) throws UnsupportedEncodingException {
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final PrintStream originalErr = System.err;
    final Thread calculatingThread = Thread.currentThread();
    OutputStream thisThreadOnly = new OutputStream() {
      @Override
      public void write(int b) {
        if (Thread.currentThread() == calculatingThread) {
          err.write(b);
        } else {
          originalErr.write(b);
        }
      }

      @Override
      public void write(byte[] b, int off, int len) {
        if (Thread.currentThread() == calculatingThread) {
          err.write(b, off, len);
        } else {
          originalErr.write(b, off, len);
        }
      }
    };
    try {
      System.setErr(new PrintStream(thisThreadOnly, true, "UTF-8"));
      new PackagingDataCalculator().calculate(tp);
    } finally {
      System.setErr(originalErr);
    }
    return err.toString("UTF-8");
  }

  private static void assertNotAvailable(ClassPackagingData cpd) {
    assertEquals("na", cpd.getCodeLocation());
    assertEquals("na", cpd.getVersion());
    assertFalse(cpd.isExact());
  }

  /**
   * Asserts the packaging data of a class loaded from a jar by the application class loader.
   */
  private static void assertPackagingDataOf(Class<?> type, ClassPackagingData cpd) {
    String location = type.getProtectionDomain().getCodeSource().getLocation().toString();
    assertTrue(location + " is not a jar", location.endsWith(".jar"));
    assertEquals(location.substring(location.lastIndexOf('/') + 1), cpd.getCodeLocation());
    String version = type.getPackage().getImplementationVersion();
    assertEquals(version == null ? "na" : version, cpd.getVersion());
    assertFalse(cpd.isExact());
  }

  private SubjectClassLoader useSubjectLoader(CodeSource codeSource) {
    SubjectClassLoader loader = new SubjectClassLoader(codeSource);
    Thread.currentThread().setContextClassLoader(loader);
    return loader;
  }

  private static CodeSource codeSourceAt(String externalForm) throws MalformedURLException {
    return new CodeSource(new LocationHandler(externalForm).url(), (Certificate[]) null);
  }

  /**
   * Makes a URL whose string form is exactly the given text, independently of the
   * platform's URL parsing, and which can be made to fail when turned into a string.
   */
  static class LocationHandler extends URLStreamHandler {
    private final String externalForm;
    private volatile boolean failing;

    LocationHandler(String externalForm) {
      this.externalForm = externalForm;
    }

    URL url() throws MalformedURLException {
      return new URL("test", null, -1, "/location", this);
    }

    void failFromNowOn() {
      failing = true;
    }

    @Override
    protected String toExternalForm(URL u) {
      if (failing) {
        throw new IllegalStateException("cannot render location");
      }
      return externalForm;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      throw new IOException("not supported");
    }
  }

  /**
   * Defines its own copy of {@link PackagingDataCalculatorSubject} with the given code
   * source, and delegates every other class to the class loader of this test.
   */
  static class SubjectClassLoader extends ClassLoader {
    private final ProtectionDomain protectionDomain;
    private Class<?> subject;

    SubjectClassLoader(CodeSource codeSource) {
      super(PackagingDataCalculatorTest.class.getClassLoader());
      this.protectionDomain = new ProtectionDomain(codeSource, null);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (!PackagingDataCalculatorSubject.class.getName().equals(name)) {
        return super.loadClass(name, resolve);
      }
      if (subject == null) {
        byte[] bytes = readClassBytes(name);
        subject = defineClass(name, bytes, 0, bytes.length, protectionDomain);
      }
      return subject;
    }

    private byte[] readClassBytes(String name) throws ClassNotFoundException {
      String resource = name.replace('.', '/') + ".class";
      try (InputStream in = getParent().getResourceAsStream(resource)) {
        if (in == null) {
          throw new ClassNotFoundException(name);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
          out.write(buffer, 0, n);
        }
        return out.toByteArray();
      } catch (IOException e) {
        throw new ClassNotFoundException(name, e);
      }
    }
  }
}
