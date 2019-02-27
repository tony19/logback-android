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
package ch.qos.logback.classic.net.server;

import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.server.AbstractServerSocketAppender;
import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 * An appender that listens on a TCP port for connections from remote
 * loggers.  Each event delivered to this appender is delivered to all
 * connected remote loggers.
 *
 * @author Carl Harris
 */
public class ServerSocketAppender
    extends AbstractServerSocketAppender<ILoggingEvent> {

  private static final PreSerializationTransformer<ILoggingEvent> pst =
      new LoggingEventPreSerializationTransformer();

  private boolean includeCallerData;

  @Override
  protected void postProcessEvent(ILoggingEvent event) {
    if (isIncludeCallerData()) {
      event.getCallerData();
    }
  }

  @Override
  protected PreSerializationTransformer<ILoggingEvent> getPST() {
    return pst;
  }

  public boolean isIncludeCallerData() {
    return includeCallerData;
  }

  public void setIncludeCallerData(boolean includeCallerData) {
    this.includeCallerData = includeCallerData;
  }

}
