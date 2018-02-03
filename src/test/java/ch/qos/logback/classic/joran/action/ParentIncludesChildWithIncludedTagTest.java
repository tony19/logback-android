/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2014, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic.joran.action;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.slf4j.Logger;

import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Verifies that {@link FindIncludeAction} class can include
 * a child XML configuration that is enclosed within an {@code <included>}
 * tag.
 */
public class ParentIncludesChildWithIncludedTagTest extends BaseIncludesTezt {

  public ParentIncludesChildWithIncludedTagTest() {
    super(RESOURCE_DIR + "parent_by_included.xml");
  }

  @Test
  public void parentIncludesChildLogcatAppender() throws JoranException {
    assertHasAppender("included-logcat", LogcatAppender.class);
  }

  @Test
  public void parentIncludesChildFileAppender() throws JoranException {
    assertHasAppender("included-trace-log", FileAppender.class);
  }

  @Test
  public void contextHasChildLoggerConfig() {
    Logger logger = context.getLogger("org.example.test.Tester");
    assertThat(logger.isInfoEnabled(), is(true));
  }
}