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
import org.slf4j.MarkerFactory;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Checks whether the marker in the event matches the marker specified by the 
 * user.
 */
public class MarkerFilter extends MatchingFilter {

  Marker markerToMatch;

  @Override
  public void start() {
    if(markerToMatch != null) {
      super.start();
    } else {
      addError("The marker property must be set for ["+getName()+"]");
    }
  }
  
  @Override
  public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format, Object[] params, Throwable t) {
    if(!isStarted()) {
      return FilterReply.NEUTRAL;
    }
    
    if(markers == null) {
      return onMismatch;
    } 
    for (Marker marker : markers) {
      if (marker.contains(markerToMatch)) {
        return onMatch;
      }
    }
    return onMismatch;
  }

  /**
   * The marker to match in the event.
   * 
   * @param markerStr
   */
  public void setMarker(String markerStr) {
    if(markerStr != null) {
      this.markerToMatch = MarkerFactory.getMarker(markerStr);
    }
  }
}
