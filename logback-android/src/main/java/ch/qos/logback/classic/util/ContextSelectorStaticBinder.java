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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.selector.DefaultContextSelector;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Holds the context selector for use in the current environment.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @since 0.9.19
 */
public class ContextSelectorStaticBinder {

  static ContextSelectorStaticBinder singleton = new ContextSelectorStaticBinder();

  ContextSelector contextSelector;
  Object key;
  
  public static ContextSelectorStaticBinder getSingleton() {
    return singleton;
  }

  /**
   * FOR INTERNAL USE. This method is intended for use by  StaticLoggerBinder.
   *  
   * @param defaultLoggerContext
   * @throws ClassNotFoundException
   * @throws NoSuchMethodException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  public void init(LoggerContext defaultLoggerContext, Object key) throws ClassNotFoundException,
      NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException  {
    if(this.key == null) {
      this.key = key;
    } else if (this.key != key) {
      throw new IllegalAccessException("Only certain classes can access this method.");
    }
    
    
    String contextSelectorStr = OptionHelper
        .getSystemProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR);
    if (contextSelectorStr == null) {
      contextSelector = new DefaultContextSelector(defaultLoggerContext);
    } else if (contextSelectorStr.equals("JNDI")) {
      throw new RuntimeException("JNDI not supported");
    } else {
      contextSelector = dynamicalContextSelector(defaultLoggerContext,
          contextSelectorStr);
    }
  }
  
  /**
   * Instantiate the context selector class designated by the user. The selector
   * must have a constructor taking a LoggerContext instance as an argument.
   * 
   * @param defaultLoggerContext
   * @param contextSelectorStr
   * @return an instance of the designated context selector class
   * @throws ClassNotFoundException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws IllegalArgumentException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  static ContextSelector dynamicalContextSelector(
      LoggerContext defaultLoggerContext, String contextSelectorStr)
      throws ClassNotFoundException, SecurityException, NoSuchMethodException,
      IllegalArgumentException, InstantiationException, IllegalAccessException,
      InvocationTargetException {
    Class<?> contextSelectorClass = Loader.loadClass(contextSelectorStr);
    Constructor cons = contextSelectorClass
        .getConstructor(new Class[] { LoggerContext.class });
    return (ContextSelector) cons.newInstance(defaultLoggerContext);
  }
  
  public ContextSelector getContextSelector() {
    return contextSelector;
  }

}
