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

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * A static utility method that converts a string that describes the
 * location of a resource into a {@link URL} object.
 *
 * @author Carl Harris
 */
public class LocationUtil {

  /** Regex pattern for a URL scheme (reference RFC 2396 section 3) */
  public static final String SCHEME_PATTERN =
      "^\\p{Alpha}[\\p{Alnum}+.-]*:.*$";

  /** Scheme name for a classpath resource */
  public static final String CLASSPATH_SCHEME = "classpath:";

  /**
   * Converts a string describing the location of a resource into a URL object.
   * @param location String describing the location
   * @return URL object that refers to {@code location}
   * @throws MalformedURLException if {@code location} is not a syntatically
   *    valid URL
   * @throws FileNotFoundException if {@code location} specifies a non-existent
   *    classpath resource
   * @throws NullPointerException if {@code location} is {@code null}
   */
  public static URL urlForResource(String location)
      throws MalformedURLException, FileNotFoundException {
    if (location == null) {
      throw new NullPointerException("location is required");
    }
    URL url = null;
    if (!location.matches(SCHEME_PATTERN)) {
      url = Loader.getResourceBySelfClassLoader(location);
    }
    else if (location.startsWith(CLASSPATH_SCHEME)) {
      String path = location.substring(CLASSPATH_SCHEME.length());
      if (path.startsWith("/")) {
        path = path.substring(1);
      }
      if (path.length() == 0) {
        throw new MalformedURLException("path is required");
      }
      url = Loader.getResourceBySelfClassLoader(path);
    }
    else {
      url = new URL(location);
    }
    if (url == null) {
      throw new FileNotFoundException(location);
    }
    return url;
  }
}
