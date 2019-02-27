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
package ch.qos.logback.classic.net;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.AbstractSSLSocketAppender;
import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 * A {@link SocketAppender} that supports SSL.
 * <p>
 * For more information on this appender, please refer to the online manual
 * at http://logback.qos.ch/manual/appenders.html#SSLSocketAppender
 *
 * @author Carl Harris
 */
public class SSLSocketAppender extends AbstractSSLSocketAppender<ILoggingEvent> {

  private final PreSerializationTransformer<ILoggingEvent> pst =
      new LoggingEventPreSerializationTransformer();

  private boolean includeCallerData;

  public SSLSocketAppender() {
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
