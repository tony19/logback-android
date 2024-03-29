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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import ch.qos.logback.classic.Level;

// http://www.riehle.org/computer-science/research/1998/ubilab-tr-1998-10-1.html

/**
 * A read-only and serializable implementation of {@link ILoggingEvent}.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @since 0.9.16
 */
public class LoggingEventVO implements ILoggingEvent, Serializable {

  private static final long serialVersionUID = 6553722650255690312L;

  private static final int NULL_ARGUMENT_ARRAY = -1;
  private static final String NULL_ARGUMENT_ARRAY_ELEMENT = "NULL_ARGUMENT_ARRAY_ELEMENT";

  private String threadName;
  private String loggerName;
  private LoggerContextVO loggerContextVO;

  private transient Level level;
  private String message;

  // we gain significant space at serialization time by marking
  // formattedMessage as transient and constructing it lazily in
  // getFormattedMessage()
  private transient String formattedMessage;

  private transient Object[] argumentArray;

  private ThrowableProxyVO throwableProxy;
  private StackTraceElement[] callerDataArray;
  private List<Marker> markers;
  private Map<String, String> mdcPropertyMap;
  private long timeStamp;

  public static LoggingEventVO build(ILoggingEvent le) {
    LoggingEventVO ledo = new LoggingEventVO();
    ledo.loggerName = le.getLoggerName();
    ledo.loggerContextVO = le.getLoggerContextVO();
    ledo.threadName = le.getThreadName();
    ledo.level = (le.getLevel());
    ledo.message = (le.getMessage());
    ledo.argumentArray = (le.getArgumentArray());
    ledo.markers = le.getMarkers();
    ledo.mdcPropertyMap = le.getMDCPropertyMap();
    ledo.timeStamp = le.getTimeStamp();
    ledo.throwableProxy = ThrowableProxyVO.build(le.getThrowableProxy());
    // add caller data only if it is there already
    // fixes http://jira.qos.ch/browse/LBCLASSIC-145
    if (le.hasCallerData()) {
      ledo.callerDataArray = le.getCallerData();
    }
    return ledo;
  }

  public String getThreadName() {
    return threadName;
  }

  public LoggerContextVO getLoggerContextVO() {
    return loggerContextVO;
  }

  public String getLoggerName() {
    return loggerName;
  }

  public Level getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public String getFormattedMessage() {
    if (formattedMessage != null) {
      return formattedMessage;
    }

    if (argumentArray != null) {
      formattedMessage = MessageFormatter.arrayFormat(message, argumentArray)
          .getMessage();
    } else {
      formattedMessage = message;
    }

    return formattedMessage;
  }

  public Object[] getArgumentArray() {
    return argumentArray;
  }

  public IThrowableProxy getThrowableProxy() {
    return throwableProxy;
  }

  public StackTraceElement[] getCallerData() {
    return callerDataArray;
  }

  public boolean hasCallerData() {
    return callerDataArray != null;
  }

  public List<Marker> getMarkers() {
    return markers;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public long getContextBirthTime() {
    return loggerContextVO.getBirthTime();
  }

  public LoggerContextVO getContextLoggerRemoteView() {
    return loggerContextVO;
  }

  public Map<String, String> getMDCPropertyMap() {
    return mdcPropertyMap;
  }
  public Map<String, String> getMdc() {
    return mdcPropertyMap;
  }
  
  public void prepareForDeferredProcessing() {
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(level.levelInt);
    if (argumentArray != null) {
      int len = argumentArray.length;
      out.writeInt(len);
      for (int i = 0; i < argumentArray.length; i++) {
        if (argumentArray[i] != null) {
          out.writeObject(argumentArray[i].toString());
        } else {
          out.writeObject(NULL_ARGUMENT_ARRAY_ELEMENT);
        }
      }
    } else {
      out.writeInt(NULL_ARGUMENT_ARRAY);
    }

  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    int levelInt = in.readInt();
    level = Level.toLevel(levelInt);

    int argArrayLen = in.readInt();
    if (argArrayLen != NULL_ARGUMENT_ARRAY) {
      argumentArray = new String[argArrayLen];
      for (int i = 0; i < argArrayLen; i++) {
        Object val = in.readObject();
        if (!NULL_ARGUMENT_ARRAY_ELEMENT.equals(val)) {
          argumentArray[i] = val;
        }
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result = prime * result
        + ((threadName == null) ? 0 : threadName.hashCode());
    result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final LoggingEventVO other = (LoggingEventVO) obj;
    if (message == null) {
      if (other.message != null)
        return false;
    } else if (!message.equals(other.message))
      return false;

    if (loggerName == null) {
      if (other.loggerName != null)
        return false;
    } else if (!loggerName.equals(other.loggerName))
      return false;

    if (threadName == null) {
      if (other.threadName != null)
        return false;
    } else if (!threadName.equals(other.threadName))
      return false;
    if (timeStamp != other.timeStamp)
      return false;

    if (markers == null) {
      if (other.markers != null)
        return false;
    } else if (!markers.equals(other.markers))
      return false;

    if (mdcPropertyMap == null) {
      if (other.mdcPropertyMap != null)
        return false;
    } else if (!mdcPropertyMap.equals(other.mdcPropertyMap))
      return false;
    return true;
  }
}
