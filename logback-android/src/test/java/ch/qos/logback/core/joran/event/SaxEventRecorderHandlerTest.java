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
package ch.qos.logback.core.joran.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;
import org.xmlpull.v1.sax2.Driver;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;

/**
 * Plain-JVM tests of {@link SaxEventRecorder} as a SAX handler, and of how
 * {@link SaxEventRecorder#recordEvents(InputSource)} reports failures.
 *
 * <p>The XML parser (Android's {@link Driver}) is replaced by a Mockito
 * construction mock, so these tests drive the handler callbacks directly.
 * Parsing real documents is covered by the Robolectric
 * {@link SaxEventRecorderTest}.
 */
public class SaxEventRecorderHandlerTest {

  private static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";
  private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";

  private final Context context = new ContextBase();
  private final SaxEventRecorder recorder = new SaxEventRecorder(context);

  private static Locator locatorAt(int line, int column) {
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(line);
    locator.setColumnNumber(column);
    return locator;
  }

  private static InputSource someInput() {
    return new InputSource(new StringReader("<x/>"));
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  private void assertStatus(Status status, int level, String message, Throwable throwable) {
    assertEquals(level, status.getLevel());
    assertEquals(message, status.getMessage());
    assertSame(throwable, status.getThrowable());
  }

  @Test
  public void defaultConstructorLeavesContextUnsetUntilSetContext() {
    SaxEventRecorder noContextRecorder = new SaxEventRecorder();
    assertNull(noContextRecorder.getContext());
    assertTrue(noContextRecorder.getSaxEventList().isEmpty());

    noContextRecorder.setContext(context);
    assertSame(context, noContextRecorder.getContext());
  }

  @Test
  public void qNameFallsBackToLocalNameWhenNullOrEmpty() {
    recorder.setDocumentLocator(locatorAt(1, 1));
    recorder.startElement("urn:x", "outer", null, new AttributesImpl());
    recorder.startElement("urn:x", "inner", "", new AttributesImpl());
    recorder.endElement("urn:x", "inner", "");
    recorder.endElement("urn:x", "outer", null);

    List<SaxEvent> events = recorder.getSaxEventList();
    assertEquals(4, events.size());
    StartEvent outer = (StartEvent) events.get(0);
    assertEquals("outer", outer.getQName());
    assertEquals("outer", outer.getLocalName());
    assertEquals("urn:x", outer.getNamespaceURI());
    assertEquals("[outer]", outer.elementPath.toString());
    StartEvent inner = (StartEvent) events.get(1);
    assertEquals("inner", inner.getQName());
    assertEquals("[outer][inner]", inner.elementPath.toString());
    assertTrue(events.get(2) instanceof EndEvent);
    assertEquals("inner", events.get(2).getQName());
    assertTrue(events.get(3) instanceof EndEvent);
    assertEquals("outer", events.get(3).getQName());
  }

  @Test
  public void elementPathUsesQNameWhenLocalNameIsNullOrEmpty() {
    recorder.setDocumentLocator(locatorAt(1, 1));
    recorder.startElement("", null, "p:outer", new AttributesImpl());
    recorder.startElement("", "", "p:inner", new AttributesImpl());

    List<SaxEvent> events = recorder.getSaxEventList();
    assertEquals("[p:outer]", ((StartEvent) events.get(0)).elementPath.toString());
    assertEquals("[p:outer][p:inner]", ((StartEvent) events.get(1)).elementPath.toString());
    assertEquals("local", recorder.getTagName("local", "p:q"));
    assertEquals("p:q", recorder.getTagName(null, "p:q"));
    assertEquals("p:q", recorder.getTagName("", "p:q"));
  }

  @Test
  public void consecutiveCharacterChunksAreAppendedToOneBodyEvent() {
    recorder.setDocumentLocator(locatorAt(2, 3));
    recorder.startElement("", "x", "x", new AttributesImpl());
    char[] chunk1 = "  hello ".toCharArray();
    char[] chunk2 = "..world..".toCharArray();
    recorder.characters(chunk1, 0, chunk1.length);
    recorder.characters(chunk2, 2, 5);

    List<SaxEvent> events = recorder.getSaxEventList();
    assertEquals(2, events.size());
    assertEquals("hello world", ((BodyEvent) events.get(1)).getText());
  }

  @Test
  public void characterDataBeforeAnyEventIsRecordedUnlessBlank() {
    recorder.setDocumentLocator(locatorAt(1, 1));
    assertNull(recorder.getLastEvent());

    char[] blank = " \n\t ".toCharArray();
    recorder.characters(blank, 0, blank.length);
    assertTrue(recorder.getSaxEventList().isEmpty());

    char[] text = "text".toCharArray();
    recorder.characters(text, 0, text.length);
    assertEquals(1, recorder.getSaxEventList().size());
    assertEquals("text", ((BodyEvent) recorder.getLastEvent()).getText());
  }

  @Test
  public void parserCallbacksReportPositionAtTheMatchingLevel() throws SAXException {
    SAXParseException spe = new SAXParseException("bad token", "pub", "sys", 3, 9);

    recorder.warning(spe);
    recorder.error(spe);
    recorder.fatalError(spe);

    List<Status> statuses = statuses();
    assertEquals(3, statuses.size());
    assertStatus(statuses.get(0), Status.WARN,
        CoreConstants.XML_PARSING + " - Parsing warning on line 3 and column 9", spe);
    assertStatus(statuses.get(1), Status.ERROR,
        CoreConstants.XML_PARSING + " - Parsing error on line 3 and column 9", spe);
    assertStatus(statuses.get(2), Status.ERROR,
        CoreConstants.XML_PARSING + " - Parsing fatal error on line 3 and column 9", spe);
  }

  @Test
  public void statusMethodsReportToTheContextWithRecorderAsOrigin() {
    Throwable t = new Exception("cause");
    Status custom = new InfoStatus("custom", "someOrigin");

    recorder.addError("e1");
    recorder.addError("e2", t);
    recorder.addInfo("i1");
    recorder.addInfo("i2", t);
    recorder.addWarn("w1");
    recorder.addWarn("w2", t);
    recorder.addStatus(custom);

    List<Status> statuses = statuses();
    assertEquals(7, statuses.size());
    assertStatus(statuses.get(0), Status.ERROR, "e1", null);
    assertStatus(statuses.get(1), Status.ERROR, "e2", t);
    assertStatus(statuses.get(2), Status.INFO, "i1", null);
    assertStatus(statuses.get(3), Status.INFO, "i2", t);
    assertStatus(statuses.get(4), Status.WARN, "w1", null);
    assertStatus(statuses.get(5), Status.WARN, "w2", t);
    for (int i = 0; i < 6; i++) {
      assertSame(recorder, statuses.get(i).getOrigin());
    }
    assertSame(custom, statuses.get(6));
  }

  @Test
  public void recordEventsIgnoresUnsupportedValidationFeatureAndReturnsRecordedEvents() throws Exception {
    try (MockedConstruction<Driver> drivers = mockConstruction(Driver.class, (driver, ctx) -> {
      doThrow(new SAXNotSupportedException("no validation"))
          .when(driver).setFeature(VALIDATION_FEATURE, false);
      doAnswer(invocation -> {
        recorder.setDocumentLocator(locatorAt(1, 1));
        recorder.startElement("", "x", "x", new AttributesImpl());
        recorder.endElement("", "x", "x");
        return null;
      }).when(driver).parse(any(InputSource.class));
    })) {
      InputSource input = someInput();
      List<SaxEvent> events = recorder.recordEvents(input);

      assertSame(recorder.getSaxEventList(), events);
      assertEquals(2, events.size());
      assertTrue(events.get(0) instanceof StartEvent);
      assertTrue(events.get(1) instanceof EndEvent);

      assertEquals(1, drivers.constructed().size());
      Driver driver = drivers.constructed().get(0);
      verify(driver).setFeature(NAMESPACES_FEATURE, true);
      verify(driver).setContentHandler(recorder);
      verify(driver).setErrorHandler(recorder);
      verify(driver).parse(input);
      assertTrue(statuses().isEmpty());
    }
  }

  @Test
  public void parserConfigurationFailureIsReportedAndNothingIsParsed() throws Exception {
    SAXNotRecognizedException failure = new SAXNotRecognizedException("no namespaces");
    try (MockedConstruction<Driver> drivers = mockConstruction(Driver.class, (driver, ctx) ->
        doThrow(failure).when(driver).setFeature(NAMESPACES_FEATURE, true))) {

      JoranException e = assertThrows(JoranException.class, () -> recorder.recordEvents(someInput()));

      assertEquals("Parser configuration error occurred", e.getMessage());
      assertSame(failure, e.getCause());
      verify(drivers.constructed().get(0), never()).parse(any(InputSource.class));
      List<Status> statuses = statuses();
      assertEquals(1, statuses.size());
      assertStatus(statuses.get(0), Status.ERROR, "Parser configuration error occurred", failure);
    }
  }

  @Test
  public void prematureEndOfInputIsReportedAsParseErrorAtLastKnownPosition() throws Exception {
    EOFException eof = new EOFException("premature end of document");
    try (MockedConstruction<Driver> ignored = mockConstruction(Driver.class, (driver, ctx) ->
        doAnswer(invocation -> {
          recorder.setDocumentLocator(locatorAt(5, 2));
          throw eof;
        }).when(driver).parse(any(InputSource.class)))) {

      JoranException e = assertThrows(JoranException.class, () -> recorder.recordEvents(someInput()));

      assertEquals("premature end of document", e.getMessage());
      SAXParseException spe = (SAXParseException) e.getCause();
      assertEquals("premature end of document", spe.getMessage());
      assertEquals(5, spe.getLineNumber());
      assertEquals(2, spe.getColumnNumber());
      assertSame(eof, spe.getException());
      List<Status> statuses = statuses();
      assertEquals(1, statuses.size());
      assertStatus(statuses.get(0), Status.ERROR, "premature end of document", spe);
    }
  }

  @Test
  public void ioFailureWhileParsingIsReported() throws Exception {
    IOException failure = new IOException("stream broke");
    try (MockedConstruction<Driver> ignored = mockConstruction(Driver.class, (driver, ctx) ->
        doThrow(failure).when(driver).parse(any(InputSource.class)))) {

      JoranException e = assertThrows(JoranException.class, () -> recorder.recordEvents(someInput()));

      assertEquals("I/O error occurred while parsing xml file", e.getMessage());
      assertSame(failure, e.getCause());
      List<Status> statuses = statuses();
      assertEquals(1, statuses.size());
      assertStatus(statuses.get(0), Status.ERROR, "I/O error occurred while parsing xml file", failure);
    }
  }

  @Test
  public void saxExceptionIsWrappedWithoutReportingItAgain() throws Exception {
    SAXException failure = new SAXException("aborted by error handler");
    try (MockedConstruction<Driver> ignored = mockConstruction(Driver.class, (driver, ctx) ->
        doThrow(failure).when(driver).parse(any(InputSource.class)))) {

      JoranException e = assertThrows(JoranException.class, () -> recorder.recordEvents(someInput()));

      assertEquals("Problem parsing XML document. See previously reported errors.", e.getMessage());
      assertSame(failure, e.getCause());
      assertTrue(statuses().isEmpty());
    }
  }

  @Test
  public void unexpectedRuntimeExceptionWhileParsingIsReported() throws Exception {
    IllegalStateException failure = new IllegalStateException("boom");
    try (MockedConstruction<Driver> ignored = mockConstruction(Driver.class, (driver, ctx) ->
        doThrow(failure).when(driver).parse(any(InputSource.class)))) {

      JoranException e = assertThrows(JoranException.class, () -> recorder.recordEvents(someInput()));

      assertEquals("Unexpected exception while parsing XML document.", e.getMessage());
      assertSame(failure, e.getCause());
      List<Status> statuses = statuses();
      assertEquals(1, statuses.size());
      assertStatus(statuses.get(0), Status.ERROR, "Unexpected exception while parsing XML document.", failure);
    }
  }
}
