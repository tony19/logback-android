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
package ch.qos.logback.classic.pattern;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.PostCompileProcessor;

public class EnsureExceptionHandling implements
    PostCompileProcessor<ILoggingEvent> {

  /**
   * This implementation checks if any of the converters in the chain handles
   * exceptions. If not, then this method adds a
   * {@link ThrowableProxyConverter} instance to the end of the chain.
   * <p>
   * This allows appenders using this layout to output exception information
   * event if the user forgets to add %ex to the pattern. Note that the
   * appenders defined in the Core package are not aware of exceptions nor
   * LoggingEvents.
   * <p>
   * If for some reason the user wishes to NOT print exceptions, then she can
   * add %nopex to the pattern.
   * 
   * 
   */
  public void process(Context context, Converter<ILoggingEvent> head) {
    if(head == null) {
      // this should never happen
      throw new IllegalArgumentException("cannot process empty chain");
    }
    if (!chainHandlesThrowable(head)) {
      Converter<ILoggingEvent> tail = ConverterUtil.findTail(head);
      Converter<ILoggingEvent> exConverter = null;
      LoggerContext loggerContext = (LoggerContext) context;
      if (loggerContext.isPackagingDataEnabled()) {
        exConverter = new ExtendedThrowableProxyConverter();
      } else {
        exConverter = new ThrowableProxyConverter();
      }
      tail.setNext(exConverter);
    }
  }

  /**
   * This method computes whether a chain of converters handles exceptions or
   * not.
   * 
   * @param head
   *                The first element of the chain
   * @return true if can handle throwables contained in logging events
   */
  public boolean chainHandlesThrowable(Converter<ILoggingEvent> head) {
    Converter<ILoggingEvent> c = head;
    while (c != null) {
      if (c instanceof ThrowableHandlingConverter) {
        return true;
      }
      c = c.getNext();
    }
    return false;
  }
}
