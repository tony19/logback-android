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
package ch.qos.logback.classic.sift;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.sift.Discriminator;
import ch.qos.logback.core.sift.SiftingAppenderBase;
import org.slf4j.Marker;

import java.util.List;

/**
 * This appender can contains other appenders which it can build dynamically
 * depending on MDC values. The built appender is specified as part of a
 * configuration file.
 * 
 * <p>See the logback manual for further details.
 * 
 * 
 * @author Ceki Gulcu
 */
public class SiftingAppender extends SiftingAppenderBase<ILoggingEvent> {

  @Override
  protected long getTimestamp(ILoggingEvent event) {
    return event.getTimeStamp();
  }
  

  @Override
  @DefaultClass(MDCBasedDiscriminator.class)
  public void setDiscriminator(Discriminator<ILoggingEvent> discriminator) {
    super.setDiscriminator(discriminator);
  }

  protected boolean eventMarksEndOfLife(ILoggingEvent event) {
    List<Marker> markers = event.getMarkers();
    if(markers == null)
      return false;

    for (Marker marker : markers) {
      if(marker.contains(ClassicConstants.FINALIZE_SESSION_MARKER))
        return true;
    }
    return false;
  }
}
