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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

/**
 * Defines its own copy of a class, and of the class's nested classes, from the
 * same class files that the original was defined from, so that the copy's
 * static initializer runs again (e.g. with other system properties). Being
 * defined from the same bytes, the copy's code coverage is recorded as the
 * original's. Every other class is loaded by the parent class loader, so the
 * copy shares them with the tests.
 *
 * <p>Used by the tests of the {@code ch.qos.logback.core.util} classes whose
 * static state can't otherwise be set up.</p>
 */
final class FreshCopyClassLoader extends ClassLoader {

  private final String className;
  private final ProtectionDomain domain;

  private FreshCopyClassLoader(Class<?> clazz) {
    super(clazz.getClassLoader());
    this.className = clazz.getName();
    this.domain = clazz.getProtectionDomain();
  }

  /**
   * Defines and initializes a copy of {@code clazz} within the original's
   * protection domain.
   *
   * @param clazz the class to copy
   * @return the initialized copy
   */
  static Class<?> initializeFreshCopy(Class<?> clazz) throws ClassNotFoundException {
    Class<?> copy = Class.forName(clazz.getName(), true, new FreshCopyClassLoader(clazz));
    if (copy == clazz) {
      throw new IllegalStateException("not a copy: " + clazz);
    }
    return copy;
  }

  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (!name.equals(className) && !name.startsWith(className + "$")) {
      return super.loadClass(name, resolve);
    }
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      byte[] bytes = readClassFile(name);
      c = defineClass(name, bytes, 0, bytes.length, domain);
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }

  private byte[] readClassFile(String name) throws ClassNotFoundException {
    InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class");
    if (in == null) {
      throw new ClassNotFoundException(name);
    }
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[4096];
      int n;
      while ((n = in.read(buf)) != -1) {
        out.write(buf, 0, n);
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new ClassNotFoundException(name, e);
    } finally {
      CloseUtil.closeQuietly(in);
    }
  }
}
