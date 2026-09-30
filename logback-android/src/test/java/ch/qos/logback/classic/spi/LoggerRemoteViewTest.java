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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;

public class LoggerRemoteViewTest {

  @Test
  public void keepsTheNameAndTheRemoteViewOfTheContext() {
    LoggerContext lc = new LoggerContext();
    lc.setName("ctx");

    LoggerRemoteView view = new LoggerRemoteView("a.b.C", lc);

    assertEquals("a.b.C", view.getName());
    assertNotNull(view.getLoggerContextView());
    assertSame(lc.getLoggerContextRemoteView(), view.getLoggerContextView());
  }

  @Test
  public void contextWithRemoteViewPassesTheAssertionWhenAssertionsAreEnabled() throws Exception {
    Constructor<?> constructor = loggerRemoteViewConstructor(true);
    LoggerContext lc = new LoggerContext();

    Object view = constructor.newInstance("a.b.C", lc);

    assertEquals("a.b.C", view.getClass().getMethod("getName").invoke(view));
    assertSame(lc.getLoggerContextRemoteView(), view.getClass().getMethod("getLoggerContextView").invoke(view));
  }

  @Test
  public void contextWithoutRemoteViewFailsTheAssertionWhenAssertionsAreEnabled() throws Exception {
    Constructor<?> constructor = loggerRemoteViewConstructor(true);

    InvocationTargetException e = assertThrows(InvocationTargetException.class,
        () -> constructor.newInstance("a.b.C", new ContextWithoutRemoteView()));
    assertTrue(String.valueOf(e.getCause()), e.getCause() instanceof AssertionError);
    assertNull(e.getCause().getMessage());
  }

  @Test
  public void contextWithoutRemoteViewYieldsNullViewWhenAssertionsAreDisabled() throws Exception {
    Constructor<?> constructor = loggerRemoteViewConstructor(false);

    Object view = constructor.newInstance("a.b.C", new ContextWithoutRemoteView());

    assertEquals("a.b.C", view.getClass().getMethod("getName").invoke(view));
    assertNull(view.getClass().getMethod("getLoggerContextView").invoke(view));
  }

  /**
   * Returns the constructor of a fresh copy of {@link LoggerRemoteView} whose assert
   * statement is enabled or disabled as requested, whatever the JVM's -ea/-da flags are.
   */
  private static Constructor<?> loggerRemoteViewConstructor(boolean assertionsEnabled) throws Exception {
    AssertionStatusClassLoader loader = new AssertionStatusClassLoader();
    // must precede the loading of the class: its assertion status is fixed when it is initialized
    loader.setClassAssertionStatus(LoggerRemoteView.class.getName(), assertionsEnabled);
    Class<?> copy = loader.loadClass(LoggerRemoteView.class.getName());
    assertNotSame(LoggerRemoteView.class, copy);
    assertEquals(assertionsEnabled, copy.desiredAssertionStatus());
    return copy.getConstructor(String.class, LoggerContext.class);
  }

  static class ContextWithoutRemoteView extends LoggerContext {
    @Override
    public LoggerContextVO getLoggerContextRemoteView() {
      return null;
    }
  }

  /**
   * Defines its own copy of {@link LoggerRemoteView}, from the same class file and code
   * source, and delegates every other class to the class loader of this test.
   */
  static class AssertionStatusClassLoader extends ClassLoader {

    AssertionStatusClassLoader() {
      super(LoggerRemoteViewTest.class.getClassLoader());
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (!LoggerRemoteView.class.getName().equals(name)) {
        return super.loadClass(name, resolve);
      }
      Class<?> copy = findLoadedClass(name);
      if (copy == null) {
        byte[] bytes = readClassBytes(name);
        copy = defineClass(name, bytes, 0, bytes.length, LoggerRemoteView.class.getProtectionDomain());
      }
      return copy;
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
