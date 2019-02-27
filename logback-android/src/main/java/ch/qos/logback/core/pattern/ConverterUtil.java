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
package ch.qos.logback.core.pattern;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;

public class ConverterUtil {

  /**
   * Start converters in the chain of converters.
   *
   * @param head head node of converter chain
   * @param <E> type of log event object
   */
  public static <E> void startConverters(Converter<E> head) {
    Converter<E> c = head;
    while (c != null) {
      // CompositeConverter is a subclass of  DynamicConverter
      if (c instanceof CompositeConverter) {
        CompositeConverter<E> cc = (CompositeConverter<E>) c;
        Converter<E> childConverter = cc.childConverter;
        startConverters(childConverter);
        cc.start();
      } else if (c instanceof DynamicConverter) {
        DynamicConverter<E> dc = (DynamicConverter<E>) c;
        dc.start();
      }
      c = c.getNext();
    }
  }


  public static <E> Converter<E> findTail(Converter<E> head) {
    Converter<E> p = head;
    while (p != null) {
      Converter<E> next = p.getNext();
      if (next == null) {
        break;
      } else {
        p = next;
      }
    }
    return p;
  }

  public static <E> void setContextForConverters(Context context, Converter<E> head) {
    Converter<E> c = head;
    while (c != null) {
      if (c instanceof ContextAware) {
        ((ContextAware) c).setContext(context);
      }
      c = c.getNext();
    }
  }
}
