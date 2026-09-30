/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.joran.action;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Test {@link PropertyAction}.
 * @author Ceki G&uuml;lc&uuml;
 */
public class PropertyActionTest  {

  Context context;
  InterpretationContext ec;
  PropertyAction propertyAction;
  DummyAttributes atts = new DummyAttributes();
  
  @Before
  public void setUp() throws Exception {
    context = new ContextBase();
    ec = new InterpretationContext(context, null);
    propertyAction = new PropertyAction();
    propertyAction.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
    context = null; 
    propertyAction = null;
    atts = null;
  }
  
  @Test
  public void nameValuePair() {
    atts.setValue("name", "v1");
    atts.setValue("value", "work");
    propertyAction.begin(ec, null, atts);
    assertEquals("work", ec.getProperty("v1"));
  }
  
  @Test
  public void nameValuePairWithPrerequisiteSubsitution() {
    context.putProperty("w", "wor");
    atts.setValue("name", "v1");
    atts.setValue("value", "${w}k");
    propertyAction.begin(ec, null, atts);
    assertEquals("work", ec.getProperty("v1"));
  }
  
  @Test
  public void noValue() {
    atts.setValue("name", "v1");
    propertyAction.begin(ec, null, atts);
    assertEquals(1, context.getStatusManager().getCount());
    assertTrue(checkError());
  }

  @Test
  public void noName() {
    atts.setValue("value", "v1");
    propertyAction.begin(ec, null, atts);
    assertEquals(1, context.getStatusManager().getCount());
    assertTrue(checkError());
  }
  
  @Test
  public void noAttributes() {
    propertyAction.begin(ec, null, atts);
    assertEquals(1, context.getStatusManager().getCount());
    assertTrue(checkError());
    StatusPrinter.print(context);
  } 
  
  @Test
  public void testFileNotLoaded() {
    atts.setValue("file", "toto");
    atts.setValue("value", "work");
    propertyAction.begin(ec, null, atts);
    assertEquals(1, context.getStatusManager().getCount());
    assertTrue(checkError());
  }
  
  @Test
  public void testLoadFileWithPrerequisiteSubsitution() {
    context.putProperty("STEM", CoreTestConstants.TEST_DIR_PREFIX + "input/joran");
    atts.setValue("file", "${STEM}/propertyActionTest.properties");
    propertyAction.begin(ec, null, atts);
    assertEquals("tata", ec.getProperty("v1"));
    assertEquals("toto", ec.getProperty("v2"));
  }

  @Test
  public void testLoadFile() {
    atts.setValue("file", CoreTestConstants.TEST_DIR_PREFIX + "input/joran/propertyActionTest.properties");
    propertyAction.begin(ec, null, atts);
    assertEquals("tata", ec.getProperty("v1"));
    assertEquals("toto", ec.getProperty("v2"));
  }

  @Test
  public void testLoadResource() {
    atts.setValue("resource", "asResource/joran/propertyActionTest.properties");
    propertyAction.begin(ec, null, atts);
    assertEquals("tata", ec.getProperty("r1"));
    assertEquals("toto", ec.getProperty("r2"));
  }
  
  @Test
  public void testLoadResourceWithPrerequisiteSubsitution() {
    context.putProperty("STEM", "asResource/joran");
    atts.setValue("resource", "${STEM}/propertyActionTest.properties");
    propertyAction.begin(ec, null, atts);
    assertEquals("tata", ec.getProperty("r1"));
    assertEquals("toto", ec.getProperty("r2"));
  }
  
  @Test
  public void testLoadNotPossible() {
    atts.setValue("file", "toto");
    propertyAction.begin(ec, null, atts);
    assertEquals(1, context.getStatusManager().getCount());
    assertTrue(checkFileErrors());
  }
  
  @Test
  public void deprecatedSubstitutionPropertyElementIsStillHonoredWithAWarning() {
    atts.setValue("name", "v1");
    atts.setValue("value", "work");
    propertyAction.begin(ec, "substitutionProperty", atts);
    assertEquals("work", ec.getProperty("v1"));
    assertEquals(1, context.getStatusManager().getCount());
    Status warning = context.getStatusManager().getCopyOfStatusList().get(0);
    assertEquals(Status.WARN, warning.getLevel());
    assertEquals("[substitutionProperty] element has been deprecated. Please use the [property] element instead.",
        warning.getMessage());
  }

  @Test
  public void propertyElementIsNotReportedAsDeprecated() {
    atts.setValue("name", "v1");
    atts.setValue("value", "work");
    propertyAction.begin(ec, "property", atts);
    assertEquals("work", ec.getProperty("v1"));
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void unreadableFileIsReported() {
    IOException failure = new IOException("disk on fire");
    useActionFailingToLoadWith(failure);
    String file = CoreTestConstants.TEST_DIR_PREFIX + "input/joran/propertyActionTest.properties";
    atts.setValue("file", file);
    propertyAction.begin(ec, null, atts);
    assertNull(ec.getProperty("v1"));
    assertOnlyError("Could not read properties file [" + file + "].", failure);
  }

  @Test
  public void unreadableResourceIsReported() {
    IOException failure = new IOException("jar on fire");
    useActionFailingToLoadWith(failure);
    atts.setValue("resource", "asResource/joran/propertyActionTest.properties");
    propertyAction.begin(ec, null, atts);
    assertNull(ec.getProperty("r1"));
    assertOnlyError("Could not read resource file [asResource/joran/propertyActionTest.properties].", failure);
  }

  @Test
  public void missingResourceIsReported() {
    context.putProperty("STEM", "asResource/joran");
    atts.setValue("resource", "${STEM}/no-such.properties");
    propertyAction.begin(ec, null, atts);
    assertOnlyError("Could not find resource [asResource/joran/no-such.properties].", null);
  }

  @Test
  public void resourcePropertiesCanBeSetInContextScope() {
    atts.setValue("resource", "asResource/joran/propertyActionTest.properties");
    atts.setValue("scope", "context");
    propertyAction.begin(ec, null, atts);
    assertEquals("tata", context.getProperty("r1"));
    assertEquals("toto", context.getProperty("r2"));
  }

  @Test
  public void fileWithNameIsInvalid() {
    atts.setValue("file", "toto");
    atts.setValue("name", "v1");
    assertInvalidAttributes();
  }

  @Test
  public void fileWithResourceIsInvalid() {
    atts.setValue("file", "toto");
    atts.setValue("resource", "asResource/joran/propertyActionTest.properties");
    assertInvalidAttributes();
    assertNull(ec.getProperty("r1"));
  }

  @Test
  public void resourceWithNameIsInvalid() {
    atts.setValue("resource", "asResource/joran/propertyActionTest.properties");
    atts.setValue("name", "v1");
    assertInvalidAttributes();
    assertNull(ec.getProperty("r1"));
  }

  @Test
  public void resourceWithValueIsInvalid() {
    atts.setValue("resource", "asResource/joran/propertyActionTest.properties");
    atts.setValue("value", "work");
    assertInvalidAttributes();
    assertNull(ec.getProperty("r1"));
  }

  @Test
  public void nameValuePairWithFileIsInvalid() {
    atts.setValue("name", "v1");
    atts.setValue("value", "work");
    atts.setValue("file", "toto");
    assertInvalidAttributes();
    assertNull(ec.getProperty("v1"));
  }

  @Test
  public void nameValuePairWithResourceIsInvalid() {
    atts.setValue("name", "v1");
    atts.setValue("value", "work");
    atts.setValue("resource", "asResource/joran/propertyActionTest.properties");
    assertInvalidAttributes();
    assertNull(ec.getProperty("v1"));
    assertNull(ec.getProperty("r1"));
  }

  @Test
  public void endAndFinishDoNothing() {
    ec.pushObject("top");
    propertyAction.end(ec, "property");
    propertyAction.finish(ec);
    assertEquals("top", ec.peekObject());
    assertEquals(1, ec.getObjectStack().size());
    assertEquals(0, context.getStatusManager().getCount());
  }

  /** Replaces the action under test by one whose reading of the opened stream fails. */
  private void useActionFailingToLoadWith(final IOException failure) {
    propertyAction = new PropertyAction() {
      @Override
      void loadAndSetProperties(InterpretationContext ec, InputStream istream, ActionUtil.Scope scope)
          throws IOException {
        istream.close();
        throw failure;
      }
    };
    propertyAction.setContext(context);
  }

  private void assertInvalidAttributes() {
    propertyAction.begin(ec, null, atts);
    assertEquals(1, context.getStatusManager().getCount());
    assertTrue(checkError());
  }

  private void assertOnlyError(String message, Throwable cause) {
    assertEquals(1, context.getStatusManager().getCount());
    Status error = context.getStatusManager().getCopyOfStatusList().get(0);
    assertEquals(Status.ERROR, error.getLevel());
    assertEquals(message, error.getMessage());
    assertSame(cause, error.getThrowable());
  }

  private boolean checkError() {
    Iterator it = context.getStatusManager().getCopyOfStatusList().iterator();
    ErrorStatus es = (ErrorStatus)it.next();
    return PropertyAction.INVALID_ATTRIBUTES.equals(es.getMessage());
  }
  
  private boolean checkFileErrors() {
    Iterator it = context.getStatusManager().getCopyOfStatusList().iterator();
    ErrorStatus es1 = (ErrorStatus)it.next();
    return "Could not find properties file [toto].".equals(es1.getMessage());
  }
}
