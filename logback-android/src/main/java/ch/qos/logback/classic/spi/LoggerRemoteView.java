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

import java.io.Serializable;

import ch.qos.logback.classic.LoggerContext;

/**
 * An interface that allows Logger objects and LoggerSer objects to be used the
 * same way be client of the LoggingEvent object.
 * <p>
 * See {@link LoggerContextVO} for the rationale of this class.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */
public class LoggerRemoteView implements Serializable {

  private static final long serialVersionUID = 5028223666108713696L;

  final LoggerContextVO loggerContextView;
  final String name;

  public LoggerRemoteView(String name, LoggerContext lc) {
    this.name = name;
    assert lc.getLoggerContextRemoteView() != null;
    loggerContextView = lc.getLoggerContextRemoteView();
  }

  public LoggerContextVO getLoggerContextView() {
    return loggerContextView;
  }

  public String getName() {
    return name;
  }
  

}
