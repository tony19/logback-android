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
package ch.qos.logback.core.sift;

import ch.qos.logback.core.spi.LifeCycle;

/**
 * Implement this interface in order to compute a discriminating value for a
 * given event of type &lt;E&gt;.
 * 
 * <p>The returned value can depend on any data available at the time of the
 * call, including data contained within the currently running thread.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 * @param <E>
 */
public interface Discriminator<E> extends LifeCycle {
  
  /**
   * Given event 'e' return a discriminating value.
   * 
   * @param e event to evaluate
   * @return discriminating value
   */
  String getDiscriminatingValue(E e);

  /**
   * The key or variable name under which the discriminating value should be
   * exported into the host environment. 
   *
   * @return key or name
   */
  String getKey();
}
