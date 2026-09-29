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
package ch.qos.logback.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;

public class CoreConstantsTest {

  private static final String JAVA_VERSION = "java.version";

  @Test
  public void hasPublicNoArgConstructor() throws Exception {
    Constructor<CoreConstants> constructor = CoreConstants.class.getConstructor();

    assertTrue(Modifier.isPublic(constructor.getModifiers()));
    assertSame(CoreConstants.class, constructor.newInstance().getClass());
  }

  @Test
  public void corePoolSizeIsOneWhenJavaVersionIsFiveOrHigher() throws Exception {
    assertEquals(1, corePoolSizeForJavaVersion("1.8.0_402"));
    assertEquals(1, corePoolSizeForJavaVersion("17.0.2"));
  }

  @Test
  public void corePoolSizeIsZeroWhenJavaVersionIsBelowFive() throws Exception {
    // Android's runtime reports a java.version of "0"
    assertEquals(0, corePoolSizeForJavaVersion("0"));
  }

  /**
   * Initializes a fresh copy of {@link CoreConstants} (in its own class loader)
   * while the {@code java.version} system property has the given value, and
   * returns that copy's {@link CoreConstants#CORE_POOL_SIZE}.
   */
  private static int corePoolSizeForJavaVersion(String javaVersion) throws Exception {
    String original = System.getProperty(JAVA_VERSION);
    System.setProperty(JAVA_VERSION, javaVersion);
    try {
      ClassLoader loader = new CoreConstantsReloadingClassLoader(CoreConstants.class.getClassLoader());
      Class<?> reloaded = Class.forName(CoreConstants.class.getName(), true, loader);
      assertNotSame(CoreConstants.class, reloaded);
      return reloaded.getField("CORE_POOL_SIZE").getInt(null);
    } finally {
      System.setProperty(JAVA_VERSION, original);
    }
  }

  /**
   * Defines its own copy of {@link CoreConstants} from the class file of the
   * parent's copy, and delegates every other class to the parent.
   */
  static class CoreConstantsReloadingClassLoader extends ClassLoader {

    CoreConstantsReloadingClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (!CoreConstants.class.getName().equals(name)) {
        return super.loadClass(name, resolve);
      }
      synchronized (this) {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          byte[] bytes = readClassFile(name);
          c = defineClass(name, bytes, 0, bytes.length);
        }
        if (resolve) {
          resolveClass(c);
        }
        return c;
      }
    }

    private byte[] readClassFile(String name) throws ClassNotFoundException {
      String resource = name.replace('.', '/') + ".class";
      InputStream in = getParent().getResourceAsStream(resource);
      if (in == null) {
        throw new ClassNotFoundException(name);
      }
      try {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
          out.write(buffer, 0, n);
        }
        return out.toByteArray();
      } catch (IOException e) {
        throw new ClassNotFoundException(name, e);
      } finally {
        try {
          in.close();
        } catch (IOException ignored) {
          // nothing more to do
        }
      }
    }
  }
}
