/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.net.testObjectBuilders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;

/**
 * A POJO-like implementation of the {@link ILoggingEvent} which allows
 * easy creation of valid logging events, which might be altered through the
 * public setters or the fluent-setters.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class MockLoggingEvent implements ILoggingEvent {

  /**
   * @return a new instance of {@link MockLoggingEvent} with some reasonable default values set
   */
  public static MockLoggingEvent create() {
    return new MockLoggingEvent();
  }

  private String threadName = "some thread name";
  private Level level = Level.DEBUG;
  private String message = "some message";
  private Object[] argumentArray = new Object[0];
  private String formattedMessage = "some formatted message";
  private String loggerName = "some logger name";
  private LoggerContextVO loggerContextVO = new LoggerContextVO("some name", new HashMap<String, String>(), 0);
  private StackTraceElement[] stackTraceElements;
  private HashMap<String, String> mdcPropertyMap = new HashMap<String, String>();
  private int timeStamp = 0;

  public MockLoggingEvent withThreadName(String threadName) {
    this.threadName = threadName;
    return this;
  }

  public MockLoggingEvent withLevel(Level level) {
    this.level = level;
    return this;
  }

  public MockLoggingEvent withMessage(String message) {
    this.message = message;
    return this;
  }

  public MockLoggingEvent withArgumentArray(Object[] argumentArray) {
    this.argumentArray = argumentArray;
    return this;
  }

  public MockLoggingEvent withFormattedMessage(String formattedMessage) {
    this.formattedMessage = formattedMessage;
    return this;
  }

  public MockLoggingEvent withLoggerName(String loggerName) {
    this.loggerName = loggerName;
    return this;
  }

  public MockLoggingEvent withLoggerContextVO(LoggerContextVO loggerContextVO) {
    this.loggerContextVO = loggerContextVO;
    return this;
  }

  public MockLoggingEvent withStackTraceElements(StackTraceElement[] stackTraceElements) {
    this.stackTraceElements = stackTraceElements;
    return this;
  }

  public MockLoggingEvent withMdcPropertyMap(HashMap<String, String> mdcPropertyMap) {
    this.mdcPropertyMap = mdcPropertyMap;
    return this;
  }

  public MockLoggingEvent withTimeStamp(int timeStamp) {
    this.timeStamp = timeStamp;
    return this;
  }

  @Override
  public String getThreadName() {
    return threadName;
  }

  @Override
  public Level getLevel() {
    return level;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Object[] getArgumentArray() {
    return argumentArray;
  }

  @Override
  public String getFormattedMessage() {
    return formattedMessage;
  }

  @Override
  public String getLoggerName() {
    return loggerName;
  }

  @Override
  public LoggerContextVO getLoggerContextVO() {
    return loggerContextVO;
  }

  @Override
  public IThrowableProxy getThrowableProxy() {
    return null;
  }

  @Override
  public StackTraceElement[] getCallerData() {

    if (stackTraceElements == null) {
      stackTraceElements = new StackTraceElement[]{new StackTraceElement("ClassName", "MethodName", "FileName", 1)};
    }

    return stackTraceElements;
  }

  @Override
  public boolean hasCallerData() {

    if (stackTraceElements == null) {
      return false;
    }

    return getCallerData().length > 0;
  }

  @Override
  public Marker getMarker() {
    return null;
  }

  @Override
  public Map<String, String> getMDCPropertyMap() {
    return mdcPropertyMap;
  }

  @Override
  public Map<String, String> getMdc() {
    return getMDCPropertyMap();
  }

  @Override
  public long getTimeStamp() {
    return timeStamp;
  }

  @Override
  public void prepareForDeferredProcessing() {
    // NOOP
  }
}
