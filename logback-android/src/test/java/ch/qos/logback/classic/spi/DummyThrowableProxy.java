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

public class DummyThrowableProxy implements IThrowableProxy {
  
  private String className;
  private String message;
  private int commonFramesCount;
  private StackTraceElementProxy[] stackTraceElementProxyArray;
  private IThrowableProxy cause;
  private IThrowableProxy[] suppressed;

  public String getClassName() {
    return className;
  }
  public void setClassName(String className) {
    this.className = className;
  }
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }
  public int getCommonFrames() {
    return commonFramesCount;
  }
  public void setCommonFramesCount(int commonFramesCount) {
    this.commonFramesCount = commonFramesCount;
  }

  public StackTraceElementProxy[] getStackTraceElementProxyArray() {
    return stackTraceElementProxyArray;
  }
  public void setStackTraceElementProxyArray(
      StackTraceElementProxy[] stackTraceElementProxyArray) {
    this.stackTraceElementProxyArray = stackTraceElementProxyArray;
  }
  public IThrowableProxy getCause() {
    return cause;
  }
  public void setCause(IThrowableProxy cause) {
    this.cause = cause;
  }

  public IThrowableProxy[] getSuppressed() {
    return suppressed;
  }

  public void setSuppressed(IThrowableProxy[] suppressed) {
    this.suppressed = suppressed;
  }
}
