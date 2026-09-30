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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;

/**
 * Unit tests for {@link Loader}.
 */
public class LoaderTest {

  private final Thread thread = Thread.currentThread();
  private final ClassLoader originalTCL = thread.getContextClassLoader();
  private final String originalIgnoreTCL = System.getProperty(Loader.IGNORE_TCL_PROPERTY_NAME);

  @After
  public void tearDown() {
    thread.setContextClassLoader(originalTCL);
    if (originalIgnoreTCL == null) {
      System.clearProperty(Loader.IGNORE_TCL_PROPERTY_NAME);
    } else {
      System.setProperty(Loader.IGNORE_TCL_PROPERTY_NAME, originalIgnoreTCL);
    }
  }

  @Test
  public void isInstantiable() {
    // the class only has static members, but its implicit constructor is public
    assertNotNull(new Loader());
  }

  @Test
  public void resourceOccurrencesAreCountedOncePerUrl() throws IOException {
    URL a = new URL("file:/logback/a.xml");
    URL b = new URL("file:/logback/b.xml");
    ClassLoader cl = new ResourcesClassLoader(Arrays.asList(a, b, a));

    Set<URL> urls = Loader.getResourceOccurrenceCount("logback.xml", cl);

    assertEquals(new HashSet<URL>(Arrays.asList(a, b)), urls);
  }

  @Test
  public void missingResourceHasNoOccurrences() throws IOException {
    ClassLoader cl = new ResourcesClassLoader(Collections.<URL>emptyList());
    assertTrue(Loader.getResourceOccurrenceCount("logback.xml", cl).isEmpty());
  }

  @Test
  public void getResourceUsesTheGivenClassLoader() {
    URL url = Loader.getResource("util/testResource.txt", getClass().getClassLoader());
    assertNotNull(url);
    assertTrue(url.toString(), url.toString().endsWith("util/testResource.txt"));
  }

  @Test
  public void getResourceReturnsNullIfTheClassLoaderFails() {
    ClassLoader failing = new ClassLoader(null) {
      @Override
      public URL getResource(String name) {
        throw new IllegalStateException("broken class loader");
      }
    };
    assertNull(Loader.getResource("util/testResource.txt", failing));
  }

  @Test
  public void getTCLReturnsTheContextClassLoader() {
    ClassLoader tcl = new ClassLoader(null) {
    };
    thread.setContextClassLoader(tcl);
    assertSame(tcl, Loader.getTCL());
  }

  @Test
  public void loadClassWithContextUsesTheClassLoaderOfTheContext() throws ClassNotFoundException {
    Context context = new ContextBase();
    assertSame(FileSize.class, Loader.loadClass(FileSize.class.getName(), context));
  }

  @Test
  public void classLoaderOfNullObjectIsRejected() {
    NullPointerException e = assertThrows(NullPointerException.class, () -> Loader.getClassLoaderOfObject(null));
    assertEquals("Argument cannot be null", e.getMessage());
  }

  @Test
  public void classLoaderOfObjectIsTheClassLoaderOfItsClass() {
    assertSame(LoaderTest.class.getClassLoader(), Loader.getClassLoaderOfObject(this));
  }

  @Test
  public void classLoaderOfBootstrapClassIsTheSystemClassLoader() {
    assertNull(String.class.getClassLoader());
    assertSame(ClassLoader.getSystemClassLoader(), Loader.getClassLoaderOfClass(String.class));
  }

  @Test
  public void loadClassPrefersTheContextClassLoader() throws ClassNotFoundException {
    thread.setContextClassLoader(new ClassLoader(null) {
      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        if ("only.in.Tcl".equals(name)) {
          return FileSize.class;
        }
        throw new ClassNotFoundException(name);
      }
    });
    assertSame(FileSize.class, Loader.loadClass("only.in.Tcl"));
  }

  @Test
  public void loadClassFallsBackToClassForNameWithoutContextClassLoader() throws ClassNotFoundException {
    thread.setContextClassLoader(null);
    assertSame(FileSize.class, Loader.loadClass(FileSize.class.getName()));
  }

  @Test
  public void loadClassFallsBackToClassForNameIfTheContextClassLoaderFails() throws ClassNotFoundException {
    RecordingClassLoader tcl = new RecordingClassLoader();
    thread.setContextClassLoader(tcl);
    assertSame(FileSize.class, Loader.loadClass(FileSize.class.getName()));
    assertEquals(Collections.singletonList(FileSize.class.getName()), tcl.requested);
  }

  @Test
  public void loadClassThrowsForUnknownClass() {
    thread.setContextClassLoader(new RecordingClassLoader());
    assertThrows(ClassNotFoundException.class, () -> Loader.loadClass("no.such.Clazz"));
  }

  @Test
  public void ignoreTCLPropertyMakesLoadClassSkipTheContextClassLoader() throws Exception {
    System.setProperty(Loader.IGNORE_TCL_PROPERTY_NAME, "true");
    Class<?> loader = initializeFreshLoader();
    assertTrue(getStaticBoolean(loader, "ignoreTCL"));

    RecordingClassLoader tcl = new RecordingClassLoader();
    thread.setContextClassLoader(tcl);
    Method loadClass = loader.getMethod("loadClass", String.class);

    assertSame(FileSize.class, loadClass.invoke(null, FileSize.class.getName()));
    assertTrue(tcl.requested.isEmpty());
  }

  @Test
  public void ignoreTCLPropertyFalseKeepsLoadClassOnTheContextClassLoader() throws Exception {
    System.setProperty(Loader.IGNORE_TCL_PROPERTY_NAME, "false");
    Class<?> loader = initializeFreshLoader();
    assertFalse(getStaticBoolean(loader, "ignoreTCL"));

    RecordingClassLoader tcl = new RecordingClassLoader();
    thread.setContextClassLoader(tcl);
    Method loadClass = loader.getMethod("loadClass", String.class);

    assertSame(FileSize.class, loadClass.invoke(null, FileSize.class.getName()));
    assertEquals(Collections.singletonList(FileSize.class.getName()), tcl.requested);
  }

  // The same as Class.getClassLoader(), on Android and on any JDK. (On the
  // JVM it used to be null unless a security policy granted logback the
  // "getClassLoader" permission, which the default policy doesn't, and which
  // AccessController never grants from JDK 24 on.)
  @SuppressWarnings("deprecation")
  @Test
  public void classLoaderAsPrivilegedIsTheClassLoaderOfTheClass() throws Exception {
    assertSame(LoaderTest.class.getClassLoader(), Loader.getClassLoaderAsPrivileged(LoaderTest.class));

    // a class defined by another class loader than the tests and Loader
    Class<?> copy = initializeFreshLoader();
    assertNotSame(Loader.class.getClassLoader(), copy.getClassLoader());
    assertSame(copy.getClassLoader(), Loader.getClassLoaderAsPrivileged(copy));
  }

  // As Class.getClassLoader() on the JVM, where the unit tests run; on
  // Android, Class.getClassLoader() returns the BootClassLoader instead.
  @SuppressWarnings("deprecation")
  @Test
  public void classLoaderAsPrivilegedOfBootstrapClassIsNull() {
    assertNull(Loader.getClassLoaderAsPrivileged(String.class));
  }

  // As Class.getClassLoader(), on the JVM and on Android.
  @SuppressWarnings("deprecation")
  @Test
  public void classLoaderAsPrivilegedOfPrimitiveTypeIsNull() {
    assertNull(Loader.getClassLoaderAsPrivileged(int.class));
  }

  /**
   * Initializes another copy of {@link Loader}, defined from the same class
   * file, so that its static initializer runs again.
   */
  private static Class<?> initializeFreshLoader() throws ClassNotFoundException {
    return FreshCopyClassLoader.initializeFreshCopy(Loader.class);
  }

  private static boolean getStaticBoolean(Class<?> clazz, String name) throws Exception {
    Field field = clazz.getDeclaredField(name);
    field.setAccessible(true);
    return field.getBoolean(null);
  }

  /** Returns the given URLs for every resource. */
  private static class ResourcesClassLoader extends ClassLoader {
    private final List<URL> urls;

    ResourcesClassLoader(List<URL> urls) {
      super(null);
      this.urls = urls;
    }

    @Override
    public Enumeration<URL> getResources(String name) {
      return Collections.enumeration(urls);
    }
  }

  /** Records the requested class names, and fails to load any of them. */
  private static class RecordingClassLoader extends ClassLoader {
    final List<String> requested = new ArrayList<String>();

    RecordingClassLoader() {
      super(null);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      requested.add(name);
      throw new ClassNotFoundException(name);
    }
  }
}
