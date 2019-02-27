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
 * A trivial class loader which throws a NoClassDefFoundError if the requested
 * class name contains the string "Bogus".
 * 
 * @author Ceki Gulcu
 */
public class BogusClassLoader extends URLClassLoader {

  public BogusClassLoader(URL[] urls) {
    super(urls);
  }

  public BogusClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, false);
  }

  /**
   * Throw NoClassDefFoundError if the requested class contains the string
   * "Bogus". Otherwise, delegate to super-class.
   */
  protected Class<?> loadClass(String name, boolean resolve)
      throws ClassNotFoundException {

    if (name.contains("Bogus")) {
      throw new NoClassDefFoundError();
    }

    return super.loadClass(name, resolve);
  }
}
