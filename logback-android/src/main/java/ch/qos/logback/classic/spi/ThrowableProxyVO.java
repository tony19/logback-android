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
import java.util.Arrays;

public class ThrowableProxyVO implements IThrowableProxy, Serializable {

  private static final long serialVersionUID = -773438177285807139L;

  private String className;
  private String message;
  private int commonFramesCount;
  private StackTraceElementProxy[] stackTraceElementProxyArray;
  private IThrowableProxy cause;
  private IThrowableProxy[] suppressed;


  public String getMessage() {
    return message;
  }
  
  public String getClassName() {
    return className;
  }

  public int getCommonFrames() {
    return commonFramesCount;
  }

  public IThrowableProxy getCause() {
    return cause;
  }
  
  public StackTraceElementProxy[] getStackTraceElementProxyArray() {
    return stackTraceElementProxyArray;
  }

  public IThrowableProxy[] getSuppressed() {
    return suppressed;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((className == null) ? 0 : className.hashCode());
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
    final ThrowableProxyVO other = (ThrowableProxyVO) obj;

    if (className == null) {
      if (other.className != null)
        return false;
    } else if (!className.equals(other.className))
      return false;

    if (!Arrays.equals(stackTraceElementProxyArray, other.stackTraceElementProxyArray))
      return false;
    
    if (!Arrays.equals(suppressed, other.suppressed))
      return false;

    if (cause == null) {
      if (other.cause != null)
        return false;
    } else if (!cause.equals(other.cause))
      return false;
    
    return true;
  }

  public static ThrowableProxyVO build(IThrowableProxy throwableProxy) {
    if(throwableProxy == null) {
      return null;
    }
    ThrowableProxyVO tpvo = new ThrowableProxyVO();
    tpvo.className = throwableProxy.getClassName();
    tpvo.message = throwableProxy.getMessage();
    tpvo.commonFramesCount = throwableProxy.getCommonFrames();
    tpvo.stackTraceElementProxyArray = throwableProxy.getStackTraceElementProxyArray();
    IThrowableProxy cause = throwableProxy.getCause();
    if(cause != null) {
      tpvo.cause = ThrowableProxyVO.build(cause);
    }
    IThrowableProxy[] suppressed = throwableProxy.getSuppressed();
    if(suppressed != null) {
      tpvo.suppressed = new IThrowableProxy[suppressed.length];
      for(int i = 0;i<suppressed.length;i++) {
        tpvo.suppressed[i] = ThrowableProxyVO.build(suppressed[i]);
      }
    }
    return tpvo;
  }
}
