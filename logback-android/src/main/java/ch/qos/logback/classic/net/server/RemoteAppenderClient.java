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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.net.server.Client;
import ch.qos.logback.core.net.server.ServerRunner;


/**
 * A client of a {@link ServerRunner} that receives events from a remote
 * appender.
 *
 * @author Carl Harris
 */
interface RemoteAppenderClient extends Client {

  /**
   * Sets the client's logger context.
   * <p>
   * This provides the local logging context to the client's service thread,
   * and is used as the destination for logging events received from the
   * client.
   * <p>
   * This method <em>must</em> be invoked before the {@link #run()} method.
   * @param lc the logger context to set
   */
  void setLoggerContext(LoggerContext lc);

}
