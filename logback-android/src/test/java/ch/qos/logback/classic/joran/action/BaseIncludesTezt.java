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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;

/**
 * Tests the {@link FindIncludeAction} class
 */
@RunWith(RobolectricTestRunner.class)
@Ignore()
public class BaseIncludesTezt {
  protected static final String OUT_DIR = ClassicTestConstants.OUTPUT_DIR_PREFIX;
  protected static final String RESOURCE_DIR = ClassicTestConstants.RESOURCES_PREFIX;

  protected LoggerContext context;
  protected JoranConfigurator config;
  private String pathToConfig;

  protected BaseIncludesTezt(String path) {
    pathToConfig = path;
  }

  @Before
  public void setup() throws JoranException {
    config = new JoranConfigurator();
    context = new LoggerContext();
    context.putProperty("OUT_DIR", OUT_DIR);
    config.setContext(context);
    config.doConfigure(pathToConfig);
  }

  @Test
  public void parentParsesChildConfig() throws JoranException {
    assertNoErrors(config);
  }

  @Test
  public void parentParsesAllChildAppenders() {
    final int childAppenderCount = 2;
    final int parentAppenderCount = 1;
    assertAppenderCount(childAppenderCount + parentAppenderCount);
  }

  protected void assertAppenderCount(int count) {
    Map<String,Object> objectMap = config.getInterpretationContext().getObjectMap();
    @SuppressWarnings("unchecked")
    Map<String,Object> appenderMap = (Map<String,Object>) objectMap.get(ActionConst.APPENDER_BAG);

    assertThat(appenderMap.size(), is(count));
  }

  protected void assertHasAppender(String name, Class<?> clazz) {
    Map<String,Object> objectMap = config.getInterpretationContext().getObjectMap();
    @SuppressWarnings("unchecked")
    Map<String,Object> appenderMap = (Map<String,Object>) objectMap.get(ActionConst.APPENDER_BAG);

    Object appender = appenderMap.get(name);
    assertThat(appender, is(instanceOf(clazz)));
  }

  protected void assertNoErrors(JoranConfigurator config) {
    List<Status> status = config.getStatusManager().getCopyOfStatusList();
    for (Status s : status) {
      assertThat(s, is(not(instanceOf(ErrorStatus.class))));
    }
  }
}