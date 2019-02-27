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
package ch.qos.logback.core.spi;

import java.io.Serializable;

/**
 * PreSerializationTransformer instances have the responsibility to transform
 * object into a presumably equivalent serializable representation.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 * @param <E>
 */
public interface PreSerializationTransformer<E> {
  Serializable transform(E event);
}
