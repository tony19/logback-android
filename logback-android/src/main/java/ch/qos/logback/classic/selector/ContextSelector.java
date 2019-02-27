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
package ch.qos.logback.classic.selector;

import java.util.List;

import ch.qos.logback.classic.LoggerContext;

/**
 * An interface that provides access to different contexts.
 * 
 * It is used by the LoggerFactory to access the context
 * it will use to retrieve loggers.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */
public interface ContextSelector {

  LoggerContext getLoggerContext();
  
  LoggerContext getLoggerContext(String name);
  
  LoggerContext getDefaultLoggerContext();
  
  LoggerContext detachLoggerContext(String loggerContextName);
  
  List<String> getContextNames();
}
