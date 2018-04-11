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
package ch.qos.logback.classic.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xml.sax.SAXParseException;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.TrivialConfigurator;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.IncludeAction;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.action.ext.StackAction;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.FileTestUtil;
import ch.qos.logback.core.util.CoreTestConstants;

@RunWith(RobolectricTestRunner.class)
public class ConditionalIncludeActionTest {

  Context context = new ContextBase();
  StatusChecker statusChecker = new StatusChecker(context);
  TrivialConfigurator tc;

  StackAction stackAction;

  static private final String INCLUSION_DIR_PREFIX = CoreTestConstants.JORAN_INPUT_PREFIX
      + "inclusion/";

  static private final String SECOND_FILE = INCLUSION_DIR_PREFIX + "second.xml";

  static private final String INCLUDED_FILE = INCLUSION_DIR_PREFIX + "included.xml";
  static private final String URL_TO_INCLUDE = "file:./" + INCLUDED_FILE;

  static private final String INVALID = INCLUSION_DIR_PREFIX + "invalid.xml";

  static private final String INCLUDED_AS_RESOURCE = "asResource/joran/inclusion/includedAsResource.xml";

  @Before
  public void setUp() throws Exception {
    FileTestUtil.makeTestOutputDir();
    HashMap<ElementSelector, Action> rulesMap = new HashMap<ElementSelector, Action>();
    rulesMap.put(new ElementSelector("x"), new NOPAction());
    rulesMap.put(new ElementSelector("x/include"), new IncludeAction());
    rulesMap.put(new ElementSelector("x/findInclude"), new FindIncludeAction());
    rulesMap.put(new ElementSelector("x/findInclude/include"), new ConditionalIncludeAction());

    stackAction = new StackAction();
    rulesMap.put(new ElementSelector("x/stack"), stackAction);

    tc = new TrivialConfigurator(rulesMap);
    tc.setContext(context);
  }

  @Test
  public void findsIncludeWithRegularInclude() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include file='"+ SECOND_FILE +"'/>" +
                "<include file='nonexistent.txt'/>" +
            "</findInclude>" +
            "<stack name='A'/>" +
            "<stack name='B'/>" +
            "<include file='"+ SECOND_FILE +"'/>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "A", "B", "SECOND", "C");
  }

  @Test
  public void findsIncludeFromBeginningOfPathList() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include file='"+ SECOND_FILE +"'/>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='"+ INVALID +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "C");
  }

  @Test
  public void findsIncludeFromMiddleOfPathList() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='"+ SECOND_FILE +"'/>" +
                "<include file='nonexistent.txt'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "C");
  }

  @Test
  public void findsIncludeFromEndOfPathList() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='"+ SECOND_FILE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "C");
  }

  @Test
  public void errorsOutForInvalidXmlAtFoundPath() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include file='"+ INVALID +"'/>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='"+ SECOND_FILE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    assertEquals(Status.ERROR, statusChecker.getHighestLevel(0));
    assertTrue(statusChecker.containsException(SAXParseException.class));
    verifyConfig("C");
  }

  @Test
  public void configHandlesMultipleFindIncludeElements() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='"+ SECOND_FILE +"'/>" +
            "</findInclude>" +
            "<findInclude>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='"+ SECOND_FILE +"'/>" +
            "</findInclude>" +
            "<stack name='B'/>" +
            "<findInclude>" +
                "<include file='nonexistent.txt'/>" +
                "<include file='"+ SECOND_FILE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "SECOND", "B", "SECOND", "C");
  }

  @Test
  public void findsIncludeFromSinglePath() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include file='"+ SECOND_FILE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "C");
  }

  @Test
  public void findsIncludeFromManyPaths() throws JoranException {
    final String NONEXIST_INCLUDES = new String(new char[1000]).replace("\0", "<include file='nonexistent.txt'/>");
    final String xml =
        "<x>" +
            "<findInclude>" +
                NONEXIST_INCLUDES +
                "<include file='"+ SECOND_FILE +"'/>" +
                NONEXIST_INCLUDES +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "C");
  }

  @Test
  public void findsIncludeFromFewPaths() throws JoranException {
    final String NONEXIST_INCLUDES = new String(new char[5]).replace("\0", "<include file='nonexistent.txt'/>");
    final String xml =
        "<x>" +
            "<findInclude>" +
                NONEXIST_INCLUDES +
                "<include file='"+ SECOND_FILE +"'/>" +
                NONEXIST_INCLUDES +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("SECOND", "C");
  }

  @Test
  public void includesResource() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include resource='"+ INCLUDED_AS_RESOURCE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("AR_A", "AR_B", "C");
  }

  @Test
  public void ignoresNonexistentResource() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include resource='nonexistent.txt'/>" +
                "<include resource='"+ INCLUDED_AS_RESOURCE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("AR_A", "AR_B", "C");
  }

  @Test
  public void includesUrl() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include url='"+ URL_TO_INCLUDE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("IA", "IB", "C");
  }

  @Test
  public void ignoresUnknownUrl() throws JoranException {
    // FIXME: This test fails when the ISP does not return 404 for
    // unknown URLs (required to cause an exception upon opening
    // the URL request). This was observed when running tests
    // while tethered to bluetooth mobile hotspot. The fix would
    // be to refactor AbstractIncludeAction to inject a URL opener.

    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include url='http://nonexistent.html'/>" +
                "<include url='"+ URL_TO_INCLUDE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("IA", "IB", "C");
  }

  @Test
  public void ignoresMalformedUrl() throws JoranException {
    final String xml =
        "<x>" +
            "<findInclude>" +
                "<include url='htp://nonexistent.html'/>" +
                "<include url='"+ URL_TO_INCLUDE +"'/>" +
            "</findInclude>" +
            "<stack name='C'/>" +
        "</x>";
    final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    tc.doConfigure(stream);
    verifyConfig("IA", "IB", "C");
  }

  void verifyConfig(String... expected) {
    Stack<String> witness = new Stack<String>();
    witness.addAll(Arrays.asList(expected));
    assertEquals(witness, stackAction.getStack());
  }
}
