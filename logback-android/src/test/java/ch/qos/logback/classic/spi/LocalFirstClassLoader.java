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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * An almost trivial no fuss implementation of a class loader following the
 * child-first delegation model.
 * 
 * @author Ceki Gulcu
 */
public class LocalFirstClassLoader extends URLClassLoader {

  public LocalFirstClassLoader(URL[] urls) {
    super(urls);
  }

  public LocalFirstClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  public void addURL(URL url) {
    super.addURL(url);
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, false);
  }

  /**
   * We override the parent-first behavior established by java.lang.Classloader.
   * 
   * The implementation is surprisingly straightforward.
   */
  protected Class<?> loadClass(String name, boolean resolve)
      throws ClassNotFoundException {

    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);

    // if not loaded, search the local (child) resources
    if (c == null) {
      try {
        c = findClass(name);
      } catch (ClassNotFoundException cnfe) {
        // ignore
      }
    }

    // if we could not find it, delegate to parent
    // Note that we don't attempt to catch any ClassNotFoundException
    if (c == null) {
      if (getParent() != null) {
        c = getParent().loadClass(name);
      } else {
        c = getSystemClassLoader().loadClass(name);
      }
    }

    if (resolve) {
      resolveClass(c);
    }

    return c;
  }
}
