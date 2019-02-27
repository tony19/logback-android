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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Allows programmatic initialization and configuration of Logback.
 * The ServiceLoader is typically used to instantiate implementations and
 * thus implementations will need to follow the guidelines of the ServiceLoader
 * specifically a no-arg constructor is required.
 */
public interface Configurator extends ContextAware {

    /**
     * The context will also be set before this method is called via
     * {@link ContextAware#setContext(ch.qos.logback.core.Context)}.
     */
    void configure(LoggerContext loggerContext);
}
