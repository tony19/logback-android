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
package ch.qos.logback.core;

import java.util.List;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterAttachableImpl;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.WarnStatus;

/**
 * Sets a skeleton implementation for appenders.
 * 
 * <p> For more information about this appender, please refer to the online
 * manual at http://logback.qos.ch/manual/appenders.html#AppenderBase
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
abstract public class AppenderBase<E> extends ContextAwareBase implements
    Appender<E> {

  protected volatile boolean started = false;

  /**
   * The guard prevents an appender from repeatedly calling its own doAppend
   * method.
   */
  private boolean guard = false;

  /**
   * Appenders are named.
   */
  protected String name;

  private FilterAttachableImpl<E> fai = new FilterAttachableImpl<E>();

  public String getName() {
    return name;
  }

  private int statusRepeatCount = 0;
  private int exceptionCount = 0;

  static final int ALLOWED_REPEATS = 5;

  public synchronized void doAppend(E eventObject) {
    // WARNING: The guard check MUST be the first statement in the
    // doAppend() method.

    // prevent re-entry.
    if (guard) {
      return;
    }

    try {
      guard = true;

      if (!this.started) {
        if (statusRepeatCount++ < ALLOWED_REPEATS) {
          addStatus(new WarnStatus(
              "Attempted to append to non started appender [" + name + "].",
              this));
        }
        return;
      }

      if (getFilterChainDecision(eventObject) == FilterReply.DENY) {
        return;
      }

      // ok, we now invoke derived class' implementation of append
      this.append(eventObject);

    } catch (Exception e) {
      if (exceptionCount++ < ALLOWED_REPEATS) {
        addError("Appender [" + name + "] failed to append.", e);
      }
    } finally {
      guard = false;
    }
  }

  abstract protected void append(E eventObject);

  /**
   * Set the name of this appender.
   */
  public void setName(String name) {
    this.name = name;
  }

  public void start() {
    started = true;
  }

  public void stop() {
    started = false;
  }

  public boolean isStarted() {
    return started;
  }

  public String toString() {
    return this.getClass().getName() + "[" + name + "]";
  }

  public void addFilter(Filter<E> newFilter) {
    fai.addFilter(newFilter);
  }

  public void clearAllFilters() {
    fai.clearAllFilters();
  }

  public List<Filter<E>> getCopyOfAttachedFiltersList() {
    return fai.getCopyOfAttachedFiltersList();
  }

  public FilterReply getFilterChainDecision(E event) {
    return fai.getFilterChainDecision(event);
  }

}
