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
package ch.qos.logback.core.pattern.color;

import ch.qos.logback.core.pattern.CompositeConverter;

/**
 * Base class for composite converters that wrap their child output in ANSI
 * foreground-color escape codes, e.g. {@code %cyan(%logger)} (issue #332).
 * Note that Android's logcat viewers generally do not render ANSI codes;
 * these converters exist mainly so configurations shared with regular
 * logback (where consoles do render them) work without errors.
 *
 * @param <E> the event type
 */
abstract public class ForegroundCompositeConverterBase<E> extends CompositeConverter<E> {

  private final static String SET_DEFAULT_COLOR =
          ANSIConstants.ESC_START + "0;" + ANSIConstants.DEFAULT_FG + ANSIConstants.ESC_END;

  @Override
  protected String transform(E event, String in) {
    StringBuilder sb = new StringBuilder();
    sb.append(ANSIConstants.ESC_START);
    sb.append(getForegroundColorCode(event));
    sb.append(ANSIConstants.ESC_END);
    sb.append(in);
    sb.append(SET_DEFAULT_COLOR);
    return sb.toString();
  }

  /**
   * Derives the ANSI foreground color code to use for the given event
   *
   * @param event the event being rendered
   * @return an ANSI foreground color code from {@link ANSIConstants}
   */
  abstract protected String getForegroundColorCode(E event);
}
