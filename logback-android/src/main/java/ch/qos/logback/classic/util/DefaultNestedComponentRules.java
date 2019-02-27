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
package ch.qos.logback.classic.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.filter.EvaluatorFilter;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.net.ssl.SSLNestedComponentRegistryRules;

/**
 * Contains mappings for the default type of nested components in
 * logback-classic.
 * 
 * @author Ceki Gulcu
 * 
 */
public class DefaultNestedComponentRules {

  static public void addDefaultNestedComponentRegistryRules(
      DefaultNestedComponentRegistry registry) {
    registry.add(AppenderBase.class, "layout", PatternLayout.class);
    registry.add(UnsynchronizedAppenderBase.class, "layout", PatternLayout.class);
    
    registry.add(AppenderBase.class, "encoder", PatternLayoutEncoder.class);
    registry.add(UnsynchronizedAppenderBase.class, "encoder", PatternLayoutEncoder.class);
    
    SSLNestedComponentRegistryRules.addDefaultNestedComponentRegistryRules(registry);
  }

}
