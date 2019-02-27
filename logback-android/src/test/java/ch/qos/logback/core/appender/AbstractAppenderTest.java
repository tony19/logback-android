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
package ch.qos.logback.core.appender;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.StatusPrinter;



abstract public class AbstractAppenderTest<E>  {
  
  
  abstract protected Appender<E> getAppender();
  abstract protected Appender<E> getConfiguredAppender();
  Context context = new ContextBase();
  
  @Test
  public void testNewAppender() {
    // new appenders should be inactive
    Appender<E> appender = getAppender();
    assertFalse( appender.isStarted()); 
  }
  
  @Test
  public void testConfiguredAppender() {
    Appender<E> appender = getConfiguredAppender();
    appender.start();
    assertTrue(appender.isStarted());
   
    appender.stop();
    assertFalse(appender.isStarted());
    
  }
  
  @Test
  public void testNoStart() {
    Appender<E> appender = getAppender();
    appender.setContext(context);
    appender.setName("doh");
    // is null OK?
    appender.doAppend(null);
    StatusChecker checker = new StatusChecker(context.getStatusManager());
    StatusPrinter.print(context);
    checker.assertContainsMatch("Attempted to append to non started appender \\[doh\\].");
  }
}


