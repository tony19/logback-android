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

import static org.junit.Assert.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNot.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;

/**
 * Tests the {@link FindIncludeAction} class
 */
public class FindIncludeActionTest {

  private static final String CHILD_CONFIG_FILENAME = "child-logback.xml";

  private static final String OUT_DIR = ClassicTestConstants.OUTPUT_DIR_PREFIX;

  private static final String CHILD_CONFIG =
        "<configuration>"
      + "  <appender name='FILE'"
      + "    class='ch.qos.logback.core.FileAppender'>"
      + "    <file>foo.log</file>"
      + "    <lazy>true</lazy>"
      + "    <layout class='ch.qos.logback.classic.PatternLayout'>"
      + "      <pattern>%msg%n</pattern>"
      + "    </layout>"
      + "  </appender>"
      + "  <root level='debug'>"
      + "    <appender-ref ref='FILE' />"
      + "  </root>"
      + "</configuration>";

  private static final String PARENT_CONFIG =
      "<configuration>"
    + "  <includes>"
    + "    <include file='"+ OUT_DIR + "/" + CHILD_CONFIG_FILENAME + "'/>"
    + "  </includes>"
    + "</configuration>";

  LoggerContext context;
  JoranConfigurator config;

  @BeforeClass
  public static void beforeClass() throws IOException {
    // create child config file to reference in our tests

    File file = new File(OUT_DIR, CHILD_CONFIG_FILENAME);
    file.getParentFile().mkdirs();
    OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
    try {
      stream.write(CHILD_CONFIG.getBytes());
    } finally {
      stream.close();
    }
  }

  @Before
  public void setup() throws JoranException {
    config = new JoranConfigurator();
    context = new LoggerContext();
    config.setContext(context);
    config.doConfigure(new ByteArrayInputStream(PARENT_CONFIG.getBytes()));

  }

  @Test
  public void parentLoadsChildWithoutError() throws JoranException {
    List<Status> status = config.getStatusManager().getCopyOfStatusList();
    for (Status s : status) {
      assertThat(s, is(not(instanceOf(ErrorStatus.class))));
    }
  }

  @Test
  public void parentIncludesChildAppender() throws JoranException {
    Map<String,Object> objectMap = config.getInterpretationContext().getObjectMap();
    @SuppressWarnings("unchecked")
    Map<String,Object> appenderMap = (Map<String,Object>) objectMap.get(ActionConst.APPENDER_BAG);

    Object appender = appenderMap.get("FILE");
    assertThat(appender, is(instanceOf(FileAppender.class)));
  }

}
