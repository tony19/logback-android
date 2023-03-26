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
package ch.qos.logback.classic.boolex;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluatorBase;

/**
 * Evaluates to true when the logging event passed as parameter contains one of
 * the user-specified markers.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class OnMarkerEvaluator extends EventEvaluatorBase<ILoggingEvent> {

  List<String> markerList = new ArrayList<String>();

  public void addMarker(String markerStr) {
    markerList.add(markerStr);
  }

  /**
   * Return true if event passed as parameter contains one of the specified
   * user-markers.
   */
  public boolean evaluate(ILoggingEvent event) throws NullPointerException,
      EvaluationException {

    List<Marker> eventsMarker = event.getMarkers();
    if (eventsMarker == null || eventsMarker.isEmpty()) {
      return false;
    }

    for (String markerStr : markerList) {
      for (Marker marker : eventsMarker) {
        if (marker.contains(markerStr)) {
          return true;
        }
      }
    }
    return false;
  }
}
