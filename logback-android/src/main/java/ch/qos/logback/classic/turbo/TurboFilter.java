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
package ch.qos.logback.classic.turbo;

import org.slf4j.Marker;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * TurboFilter is a specialized filter with a decide method that takes a bunch
 * of parameters instead of a single event object. The latter is cleaner but
 * the first is much more performant.
 * <p>
 * For more information about turbo filters, please refer to the online manual at
 * http://logback.qos.ch/manual/filters.html#TurboFilter
 *
 * @author Ceki Gulcu
 */
public abstract class TurboFilter extends ContextAwareBase implements LifeCycle {

  private String name;
  boolean start = false;


  /**
   * Make a decision based on the multiple parameters passed as arguments.
   * The returned value should be one of <code>{@link FilterReply#DENY}</code>,
   * <code>{@link FilterReply#NEUTRAL}</code>, or <code>{@link FilterReply#ACCEPT}</code>.

   * @param markers
   * @param logger
   * @param level
   * @param format
   * @param params
   * @param t
   * @return decision
   */
  public abstract FilterReply decide(List<Marker> markers, Logger logger,
                                     Level level, String format, Object[] params, Throwable t);

  public void start() {
    this.start = true;
  }

  public boolean isStarted() {
    return this.start;
  }

  public void stop() {
    this.start = false;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
