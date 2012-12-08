/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic.android.encoder;

import ch.qos.logback.classic.android.TagPatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

public class TagPatternLayoutEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {

  @Override
  public void start() {
    TagPatternLayout patternLayout = new TagPatternLayout();
    patternLayout.setContext(context);
    patternLayout.setPattern(getPattern());
    patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
    patternLayout.start();
    this.layout = patternLayout;
    super.start();
  }
   
}
