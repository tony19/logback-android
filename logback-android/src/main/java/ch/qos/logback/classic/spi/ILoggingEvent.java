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
package ch.qos.logback.classic.spi;

import java.util.List;
import java.util.Map;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.spi.DeferredProcessingAware;

/**
 * The central interface in logback-classic. In a nutshell, logback-classic is
 * nothing more than a processing chain built around this interface.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @since 0.9.16
 */
public interface ILoggingEvent extends DeferredProcessingAware {

  String getThreadName();

  Level getLevel();

  String getMessage();

  Object[] getArgumentArray();

  String getFormattedMessage();

  String getLoggerName();

  LoggerContextVO getLoggerContextVO();

  IThrowableProxy getThrowableProxy();

  /**
   * Return caller data associated with this event. Note that calling this event
   * may trigger the computation of caller data.
   * 
   * @return the caller data associated with this event.
   * 
   * @see #hasCallerData()
   */
  StackTraceElement[] getCallerData();

  /**
   * If this event has caller data, then true is returned. Otherwise the
   * returned value is null.
   * 
   * <p>Logback components wishing to use caller data if available without
   * causing it to be computed can invoke this method before invoking
   * {@link #getCallerData()}.
   * 
   * @return whether this event has caller data
   */
  boolean hasCallerData();

  List<Marker> getMarkers();

  /**
   * @return the MDC map. The returned value can be an empty map but not null.
   */
  Map<String, String> getMDCPropertyMap();

  /**
   * Synonym for [@link #getMDCPropertyMap}.
   * @return a map of MDC properties to values
   * @deprecated  Replaced by [@link #getMDCPropertyMap}
   */
  Map<String, String> getMdc();
  long getTimeStamp();

  void prepareForDeferredProcessing();

}
