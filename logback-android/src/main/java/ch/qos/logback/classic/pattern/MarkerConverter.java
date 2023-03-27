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

import org.slf4j.Marker;

import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Return the event's marker value(s).
 * 
 * @author S&eacute;bastien Pennec
 */
public class MarkerConverter extends ClassicConverter {

  private static String EMPTY = "";

  public String convert(ILoggingEvent le) {
    List<Marker> markers = le.getMarkers();
    if (markers == null || markers.isEmpty()) {
      return EMPTY;
    } else {
      return markers.toString();
    }
  }

}
