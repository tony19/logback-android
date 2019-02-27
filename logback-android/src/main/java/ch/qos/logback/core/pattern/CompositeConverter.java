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

abstract public class CompositeConverter<E> extends DynamicConverter<E> {

  Converter<E> childConverter;

  public String convert(E event) {
    StringBuilder buf = new StringBuilder();

    for (Converter<E> c = childConverter; c != null; c = c.next) {
      c.write(buf, event);
    }
    String intermediary = buf.toString();
    return transform(event, intermediary);
  }

  abstract protected String transform(E event, String in);

  public Converter<E> getChildConverter() {
    return childConverter;
  }

  public void setChildConverter(Converter<E> child) {
    childConverter = child;
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("CompositeConverter<");

    if (formattingInfo != null)
      buf.append(formattingInfo);

    if (childConverter != null) {
      buf.append(", children: ").append(childConverter);
    }
    buf.append(">");
    return buf.toString();
  }
}
