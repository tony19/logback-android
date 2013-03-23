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
package ch.qos.logback.classic.android;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.Loader;

public class ASaxEventRecorderTest {
  static private LoggerContext context;
  static private ASaxEventRecorder recorder;
  static private ClassLoader classLoader;
  static private InputStream stream;

  @BeforeClass
  static public void beforeClass() {
    context = new LoggerContext();
    recorder = new ASaxEventRecorder();
    classLoader = Loader.getClassLoaderOfObject(ASaxEventRecorderTest.class);
    recorder.setContext(context);
  }

  @Before
  public void before() {
    stream = classLoader.getResourceAsStream("asResource/AndroidManifest.xml");
  }

  @After
  public void after() throws IOException {
    if (stream != null) {
      stream.close();
    }
  }

  @Test
  public void setFilterOnlyRecordsSpecifiedEvents() throws JoranException {
    recorder.setFilter("manifest", "logback");
    recorder.recordEvents(stream);
    List<SaxEvent> events = recorder.getSaxEventList();
    assertFalse(events.isEmpty());
  }
}
