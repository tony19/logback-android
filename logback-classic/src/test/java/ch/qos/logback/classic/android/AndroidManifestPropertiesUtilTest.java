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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.TrivialConfigurator;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.action.ext.StackAction;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.ElementSelector;

/**
 * Integration tests for {@link AndroidManifestPropertiesUtil}
 */
public class AndroidManifestPropertiesUtilTest {

  // Package name is "android" when running on PC because the "AndroidManifest.xml"
  // is read from the android.jar. Assume 2.1 SDK.
  private static final String PACKAGE_VAL = "android";
  private static final String VERSION_NAME_VAL = "2.1";
  private static final String VERSION_CODE_VAL = ""; // no version code for android.jar
  private static final String EXT_DIR_VAL = "/mnt/sdcard";
  private static final String DATA_DIR_VAL = "/data/data/" + PACKAGE_VAL + "/files";

  private static Context context = new LoggerContext();;
  private TrivialConfigurator tc;
  private StackAction stackAction;

  @Before
  public void setup() {
    HashMap<ElementSelector, Action> rulesMap = new HashMap<ElementSelector, Action>();
    rulesMap.put(new ElementSelector("x"), new NOPAction());

    stackAction = new StackAction();
    rulesMap.put(new ElementSelector("x/stack"), stackAction);

    tc = new TrivialConfigurator(rulesMap);
    tc.setContext(context);
  }

  @Test
  public void extDirKeyFound() throws JoranException {
    tc.doConfigure(toXml(CoreConstants.EXT_DIR_KEY));
    assertResultContains(EXT_DIR_VAL);
  }

  @Test
  public void dataDirKeyFound() throws JoranException {
    tc.doConfigure(toXml(CoreConstants.DATA_DIR_KEY));
    assertResultContains(DATA_DIR_VAL);
  }

  @Test
  public void versionNameKeyFound() throws JoranException {
    tc.doConfigure(toXml(CoreConstants.VERSION_NAME_KEY));
    String ver = stackAction.getStack().pop();
    // check first part of version name only; "2.1-update1" is
    // equivalent to "2.1" for our test purposes
    assertEquals(VERSION_NAME_VAL, ver.substring(0, VERSION_NAME_VAL.length()));
  }

  @Test
  public void versionCodeKeyFound() throws JoranException {
    tc.doConfigure(toXml(CoreConstants.VERSION_CODE_KEY));
    assertResultContains(VERSION_CODE_VAL);
  }

  @Test
  public void multipleAndroidKeysFound() throws JoranException {
    tc.doConfigure(
        toXml(CoreConstants.EXT_DIR_KEY,
              CoreConstants.PACKAGE_NAME_KEY,
              CoreConstants.DATA_DIR_KEY,
              CoreConstants.VERSION_CODE_KEY
              )
    );

    assertResultContains(EXT_DIR_VAL,
                         PACKAGE_VAL,
                         DATA_DIR_VAL,
                         VERSION_CODE_VAL);
  }

  /**
   * Verifies that the stack -- processed from the TrivialConfigurator -- contains
   * the expected values. Asserts if any value is missing.
   *
   * @param expected expected values
   */
  private void assertResultContains(String... expected) {
    Stack<String> witness = new Stack<String>();
    witness.addAll(Arrays.asList(expected));
    assertEquals(witness, stackAction.getStack());
  }

  /**
   * Gets an XML block with "stack" elements, each containing a variable
   * reference. This is meant to be passed to TrivialConfigurator for
   * parsing variables.
   *
   * @param names names of the variables in the stack elements. A new stack
   * element is created for each name.
   * @return a ByteArrayInputStream, containing the XML block with stack
   * elements
   */
  private ByteArrayInputStream toXml(String... names) {
    StringBuffer buf = new StringBuffer();
    buf.append("<x>");
    for(String x : names) {
      buf.append("<stack name='${" + x + "}'/>");
    }
    buf.append("</x>");
    return new ByteArrayInputStream(buf.toString().getBytes());
  }
}
