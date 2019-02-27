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

import java.util.HashSet;
import java.util.Set;

import ch.qos.logback.core.spi.LifeCycle;

/**
 * An object that manages a collection of components that implement the
 * {@link LifeCycle} interface.  Each component that is added to the manager
 * will be stopped and removed from the manager when the manager is reset.
 *
 * @author Carl Harris
 */
public class LifeCycleManager {

  private final Set<LifeCycle> components = new HashSet<LifeCycle>();
  /**
   * Registers a component with this manager.
   * <p>
   * @param component the component whose life cycle is to be managed
   */
  public void register(LifeCycle component) {
    components.add(component);
  }

  /**
   * Resets this manager.
   * <p>
   * All registered components are stopped and removed from the manager.
   */
  public void reset() {
    for (LifeCycle component : components) {
      if (component.isStarted()) {
        component.stop();
      }
    }
    components.clear();
  }
}
