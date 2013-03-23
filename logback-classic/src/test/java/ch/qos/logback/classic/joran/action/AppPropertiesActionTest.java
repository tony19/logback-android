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
package ch.qos.logback.classic.joran.action;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.CommonPathUtil;
import ch.qos.logback.core.joran.TrivialConfigurator;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.action.ext.StackAction;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.Pattern;

public class AppPropertiesActionTest {

  // package name is "android" when running on PC because the "AndroidManifest.xml"
  // is read from the android.jar
  private static final String PACKAGE_NAME = "android";

  private static final String WITH_ANDROID_PROPS_XML =
      "<x>" +
        "<appProps/>" +
        "<stack name='${" + CoreConstants.EXT_DIR_KEY + "}'/>" +
        "<stack name='${" + CoreConstants.PACKAGE_KEY + "}'/>" +
        "<stack name='${" + CoreConstants.DATA_DIR_KEY + "}'/>" +
      "</x>";

  private static final String NO_ANDROID_PROPS_XML =
      "<x>" +
          "<stack name='${" + CoreConstants.EXT_DIR_KEY + "}'/>" +
          "<stack name='${" + CoreConstants.PACKAGE_KEY + "}'/>" +
          "<stack name='${" + CoreConstants.DATA_DIR_KEY + "}'/>" +
      "</x>";

  private Context context = new ContextBase();
  private TrivialConfigurator tc;
  private StackAction stackAction;

  @Before
  public void setup() {
    HashMap<Pattern, Action> rulesMap = new HashMap<Pattern, Action>();
    rulesMap.put(new Pattern("x"), new NOPAction());
    rulesMap.put(new Pattern("x/appProps"), new AppPropertiesAction());

    stackAction = new StackAction();
    rulesMap.put(new Pattern("x/stack"), stackAction);

    tc = new TrivialConfigurator(rulesMap);
    tc.setContext(context);
  }

  @Test
  public void variablesPopulatedWhenAndroidPropsElementExists() throws JoranException {
    final ByteArrayInputStream stream = new ByteArrayInputStream(WITH_ANDROID_PROPS_XML.getBytes());
    tc.doConfigure(stream);
    verifyConfig(CommonPathUtil.getMountedExternalStorageDirectoryPath(),
                 PACKAGE_NAME,
                 CommonPathUtil.getFilesDirectoryPath(PACKAGE_NAME));
  }

  @Test
  public void variablesUndefinedWhenAndroidPropsElementDoesNotExist() throws JoranException {
    final ByteArrayInputStream stream = new ByteArrayInputStream(NO_ANDROID_PROPS_XML.getBytes());
    tc.doConfigure(stream);
    verifyConfig(CoreConstants.EXT_DIR_KEY + "_IS_UNDEFINED",
                 CoreConstants.PACKAGE_KEY + "_IS_UNDEFINED",
                 CoreConstants.DATA_DIR_KEY +"_IS_UNDEFINED");
  }

  private void verifyConfig(String... expected) {
    Stack<String> witness = new Stack<String>();
    witness.addAll(Arrays.asList(expected));
    assertEquals(witness, stackAction.getStack());
  }
}
