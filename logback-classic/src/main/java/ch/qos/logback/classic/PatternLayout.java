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
package ch.qos.logback.classic;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.pattern.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.pattern.parser.Parser;

/**
 * <p>
 * A flexible layout configurable with pattern string. The goal of this class is
 * to format a {@link ILoggingEvent} and return the results in a {#link String}.
 * The format of the result depends on the <em>conversion pattern</em>.
 * <p>
 * For more information about this layout, please refer to the online manual at
 * http://logback.qos.ch/manual/layouts.html#PatternLayout
 *
 */

public class PatternLayout extends PatternLayoutBase<ILoggingEvent> {

  public static final Map<String, String> defaultConverterMap = new HashMap<String, String>();

  static {
    defaultConverterMap.putAll(Parser.DEFAULT_COMPOSITE_CONVERTER_MAP);

    defaultConverterMap.put("d", DateConverter.class.getName());
    defaultConverterMap.put("date", DateConverter.class.getName());

    defaultConverterMap.put("r", RelativeTimeConverter.class.getName());
    defaultConverterMap.put("relative", RelativeTimeConverter.class.getName());

    defaultConverterMap.put("level", LevelConverter.class.getName());
    defaultConverterMap.put("le", LevelConverter.class.getName());
    defaultConverterMap.put("p", LevelConverter.class.getName());

    defaultConverterMap.put("t", ThreadConverter.class.getName());
    defaultConverterMap.put("thread", ThreadConverter.class.getName());

    defaultConverterMap.put("lo", LoggerConverter.class.getName());
    defaultConverterMap.put("logger", LoggerConverter.class.getName());
    defaultConverterMap.put("c", LoggerConverter.class.getName());

    defaultConverterMap.put("m", MessageConverter.class.getName());
    defaultConverterMap.put("msg", MessageConverter.class.getName());
    defaultConverterMap.put("message", MessageConverter.class.getName());

    defaultConverterMap.put("C", ClassOfCallerConverter.class.getName());
    defaultConverterMap.put("class", ClassOfCallerConverter.class.getName());

    defaultConverterMap.put("M", MethodOfCallerConverter.class.getName());
    defaultConverterMap.put("method", MethodOfCallerConverter.class.getName());

    defaultConverterMap.put("L", LineOfCallerConverter.class.getName());
    defaultConverterMap.put("line", LineOfCallerConverter.class.getName());

    defaultConverterMap.put("F", FileOfCallerConverter.class.getName());
    defaultConverterMap.put("file", FileOfCallerConverter.class.getName());

    defaultConverterMap.put("X", MDCConverter.class.getName());
    defaultConverterMap.put("mdc", MDCConverter.class.getName());

    defaultConverterMap.put("ex", ThrowableProxyConverter.class.getName());
    defaultConverterMap.put("exception", ThrowableProxyConverter.class
        .getName());
    defaultConverterMap.put("rEx", RootCauseFirstThrowableProxyConverter.class.getName());
    defaultConverterMap.put("rootException", RootCauseFirstThrowableProxyConverter.class
        .getName());
    defaultConverterMap.put("throwable", ThrowableProxyConverter.class
        .getName());

    defaultConverterMap.put("xEx", ExtendedThrowableProxyConverter.class.getName());
    defaultConverterMap.put("xException", ExtendedThrowableProxyConverter.class
        .getName());
    defaultConverterMap.put("xThrowable", ExtendedThrowableProxyConverter.class
        .getName());

    defaultConverterMap.put("nopex", NopThrowableInformationConverter.class
        .getName());
    defaultConverterMap.put("nopexception",
        NopThrowableInformationConverter.class.getName());

    defaultConverterMap.put("cn", ContextNameConverter.class.getName());
    defaultConverterMap.put("contextName", ContextNameConverter.class.getName());

    defaultConverterMap.put("caller", CallerDataConverter.class.getName());

    defaultConverterMap.put("marker", MarkerConverter.class.getName());

    defaultConverterMap.put("property", PropertyConverter.class.getName());

    defaultConverterMap.put("n", LineSeparatorConverter.class.getName());

    defaultConverterMap.put("lsn", LocalSequenceNumberConverter.class.getName());
  }

  public PatternLayout() {
    this.postCompileProcessor = new EnsureExceptionHandling();
  }

  public Map<String, String> getDefaultConverterMap() {
    return defaultConverterMap;
  }

  public String doLayout(ILoggingEvent event) {
    if (!isStarted()) {
      return CoreConstants.EMPTY_STRING;
    }
    return writeLoopOnConverters(event);
  }
}
