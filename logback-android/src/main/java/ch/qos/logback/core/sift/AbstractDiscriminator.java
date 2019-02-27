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

import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Base implementation of {@link Discriminator} that provides basic lifecycle management
 *
 * @author Tomasz Nurkiewicz
 * @since 3/29/13, 3:28 PM
 */
public abstract class AbstractDiscriminator<E> extends ContextAwareBase implements Discriminator<E> {

  protected boolean started;

  public void start() {
    started = true;
  }

  public void stop() {
    started = false;
  }

  public boolean isStarted() {
    return started;
  }
}
