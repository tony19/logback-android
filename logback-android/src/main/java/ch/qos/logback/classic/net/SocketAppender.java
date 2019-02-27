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
// Contributors: Dan MacDonald <dan@redknee.com>
package ch.qos.logback.classic.net;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 * Sends {@link ILoggingEvent} objects to a remote a log server, usually a
 * {@link SocketNode}.
 *
 * For more information on this appender, please refer to the online manual
 * at http://logback.qos.ch/manual/appenders.html#SocketAppender
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */

public class SocketAppender extends AbstractSocketAppender<ILoggingEvent> {

  private static final PreSerializationTransformer<ILoggingEvent> pst =
      new LoggingEventPreSerializationTransformer();

  private boolean includeCallerData = false;

  public SocketAppender() {
  }


  @Override
  protected void postProcessEvent(ILoggingEvent event) {
    if (includeCallerData) {
      event.getCallerData();
    }
  }

  public void setIncludeCallerData(boolean includeCallerData) {
    this.includeCallerData = includeCallerData;
  }

  public PreSerializationTransformer<ILoggingEvent> getPST() {
    return pst;
  }

}
