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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.Attributes;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Loader;

/**
 * Tests {@link AbstractIncludeAction} through a subclass that records what it
 * is asked to include.
 */
public class AbstractIncludeActionTest {

  static final String RESOURCE = "asResource/joran/inclusion/includedAsResource.xml";

  static final String NO_SOURCE_MESSAGE = "One of \"file\", \"resource\" or \"url\" attributes must be set.";
  static final String SEVERAL_SOURCES_MESSAGE = "Only one of \"file\", \"resource\" or \"url\" attributes should be set.";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  Context context = new ContextBase();
  InterpretationContext ic = new InterpretationContext(context, null);
  RecordingIncludeAction action = new RecordingIncludeAction();
  DummyAttributes atts = new DummyAttributes();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void existingFileIsIncluded() throws Exception {
    File file = tmp.newFile("included.xml");
    atts.setValue("file", file.getPath());

    action.begin(ic, "include", atts);

    assertEquals(Collections.singletonList(file.toURI().toURL()), action.includedUrls);
    assertEquals(file.getPath(), action.getAttributeInUse());
    assertFalse(action.isOptional());
    assertNoStatus();
  }

  @Test
  public void fileAttributeIsSubstitutedBeforeUse() throws Exception {
    File file = tmp.newFile("included.xml");
    context.putProperty("includeDir", tmp.getRoot().getPath());
    atts.setValue("file", "${includeDir}/included.xml");

    action.begin(ic, "include", atts);

    String expectedPath = tmp.getRoot().getPath() + "/included.xml";
    assertEquals(expectedPath, action.getAttributeInUse());
    assertEquals(Collections.singletonList(new File(expectedPath).toURI().toURL()), action.includedUrls);
    assertEquals(file.toURI().toURL(), action.includedUrls.get(0));
  }

  @Test
  public void missingFileIsAWarning() throws Exception {
    String path = new File(tmp.getRoot(), "missing.xml").getPath();
    atts.setValue("file", path);

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    Status status = assertStatus(Status.WARN, "File does not exist [" + path + "]");
    assertTrue(status.getThrowable() instanceof FileNotFoundException);
    assertEquals(path, status.getThrowable().getMessage());
  }

  @Test
  public void directoryIsNotIncluded() throws Exception {
    String path = tmp.newFolder("dir").getPath();
    atts.setValue("file", path);

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    assertStatus(Status.WARN, "File does not exist [" + path + "]");
  }

  @Test
  public void missingOptionalFileIsSilentlySkipped() throws Exception {
    atts.setValue("file", new File(tmp.getRoot(), "missing.xml").getPath());
    atts.setValue("optional", "true");

    action.begin(ic, "include", atts);

    assertTrue(action.isOptional());
    assertTrue(action.includedUrls.isEmpty());
    assertNoStatus();
  }

  @Test
  public void noSourceAttributeIsAnError() throws Exception {
    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    assertNull(action.getAttributeInUse());
    Status status = assertStatus(Status.ERROR, NO_SOURCE_MESSAGE);
    assertNull(status.getThrowable());
  }

  @Test
  public void noSourceAttributeIsSilentWhenOptional() throws Exception {
    atts.setValue("optional", "true");

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    assertNoStatus();
  }

  @Test
  public void severalSourceAttributesAreAnError() throws Exception {
    File file = tmp.newFile("included.xml");
    atts.setValue("file", file.getPath());
    atts.setValue("resource", RESOURCE);

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    assertNull(action.getAttributeInUse());
    Status status = assertStatus(Status.ERROR, SEVERAL_SOURCES_MESSAGE);
    assertNull(status.getThrowable());
  }

  @Test
  public void severalSourceAttributesAreSilentWhenOptional() throws Exception {
    atts.setValue("url", "file:/a.xml");
    atts.setValue("resource", RESOURCE);
    atts.setValue("optional", "true");

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    assertNoStatus();
  }

  @Test
  public void reachableUrlIsIncluded() throws Exception {
    URL url = tmp.newFile("included.xml").toURI().toURL();
    atts.setValue("url", url.toString());

    action.begin(ic, "include", atts);

    assertEquals(Collections.singletonList(url), action.includedUrls);
    assertEquals(url.toString(), action.getAttributeInUse());
    assertNoStatus();
  }

  @Test
  public void urlAttributeIsSubstitutedBeforeUse() throws Exception {
    URL url = tmp.newFile("included.xml").toURI().toURL();
    context.putProperty("includeUrl", url.toString());
    atts.setValue("url", "${includeUrl}");

    action.begin(ic, "include", atts);

    assertEquals(url.toString(), action.getAttributeInUse());
    assertEquals(Collections.singletonList(url), action.includedUrls);
    assertNoStatus();
  }

  @Test
  public void malformedUrlIsAnError() throws Exception {
    atts.setValue("url", "htp://logback.qos.ch");

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    Status status = assertStatus(Status.ERROR, "URL [htp://logback.qos.ch] is not well formed.");
    assertTrue(status.getThrowable() instanceof MalformedURLException);
  }

  @Test
  public void unopenableUrlIsAWarning() throws Exception {
    String url = new File(tmp.getRoot(), "missing.xml").toURI().toURL().toString();
    atts.setValue("url", url);

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    Status status = assertStatus(Status.WARN, "URL [" + url + "] cannot be opened.");
    assertTrue(status.getThrowable() instanceof FileNotFoundException);
  }

  @Test
  public void resourceIsIncluded() throws Exception {
    URL url = Loader.getResourceBySelfClassLoader(RESOURCE);
    assertNotNull(url);
    atts.setValue("resource", RESOURCE);

    action.begin(ic, "include", atts);

    assertEquals(Collections.singletonList(url), action.includedUrls);
    assertEquals(RESOURCE, action.getAttributeInUse());
    assertNoStatus();
  }

  @Test
  public void resourceAttributeIsSubstitutedBeforeUse() throws Exception {
    context.putProperty("includeResource", RESOURCE);
    atts.setValue("resource", "${includeResource}");

    action.begin(ic, "include", atts);

    assertEquals(RESOURCE, action.getAttributeInUse());
    assertEquals(Collections.singletonList(Loader.getResourceBySelfClassLoader(RESOURCE)), action.includedUrls);
    assertNoStatus();
  }

  @Test
  public void emptySourceAttributesCountAsUnset() throws Exception {
    atts.setValue("file", "");
    atts.setValue("url", "");
    atts.setValue("resource", RESOURCE);

    action.begin(ic, "include", atts);

    assertEquals(RESOURCE, action.getAttributeInUse());
    assertEquals(Collections.singletonList(Loader.getResourceBySelfClassLoader(RESOURCE)), action.includedUrls);
    assertNoStatus();
  }

  @Test
  public void missingResourceIsAnError() throws Exception {
    atts.setValue("resource", "no/such/resource.xml");

    action.begin(ic, "include", atts);

    assertTrue(action.includedUrls.isEmpty());
    Status status = assertStatus(Status.ERROR, "Could not find resource corresponding to [no/such/resource.xml]");
    assertNull(status.getThrowable());
  }

  @Test
  public void failureToProcessTheIncludedDocumentIsAnError() throws Exception {
    File file = tmp.newFile("included.xml");
    action.failure = new JoranException("bad document");
    atts.setValue("file", file.getPath());

    action.begin(ic, "include", atts);

    assertEquals(1, action.includedUrls.size());
    Status status = assertStatus(Status.ERROR, "Error while parsing " + file.getPath());
    assertSame(action.failure, status.getThrowable());
  }

  @Test
  public void failureToProcessAnOptionalDocumentIsSilent() throws Exception {
    File file = tmp.newFile("included.xml");
    action.failure = new JoranException("bad document");
    atts.setValue("file", file.getPath());
    atts.setValue("optional", "true");

    action.begin(ic, "include", atts);

    assertEquals(1, action.includedUrls.size());
    assertNoStatus();
  }

  @Test
  public void optionalFlagIsReadAgainOnEachBegin() throws Exception {
    atts.setValue("optional", "true");
    action.begin(ic, "include", atts);
    assertTrue(action.isOptional());

    atts.setValue("optional", "false");
    action.begin(ic, "include", atts);

    assertFalse(action.isOptional());
    assertStatus(Status.ERROR, NO_SOURCE_MESSAGE);
  }

  @Test
  public void attributeInUseIsForgottenOnEachBegin() throws Exception {
    File file = tmp.newFile("included.xml");
    atts.setValue("file", file.getPath());
    action.begin(ic, "include", atts);
    assertEquals(file.getPath(), action.getAttributeInUse());

    atts.setValue("file", null);
    action.begin(ic, "include", atts);

    assertNull(action.getAttributeInUse());
    assertEquals(1, action.includedUrls.size());
    assertStatus(Status.ERROR, NO_SOURCE_MESSAGE);
  }

  @Test
  public void sourceAttributeVanishingAfterTheCheckIsAnIllegalState() {
    Attributes changing = mock(Attributes.class);
    when(changing.getValue("resource")).thenReturn(RESOURCE, (String) null);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> action.begin(ic, "include", changing));

    assertEquals("A URL stream should have been returned", e.getMessage());
    assertTrue(action.includedUrls.isEmpty());
  }

  @Test
  public void notFoundErrorsAreReportedAsWarnings() {
    FileNotFoundException notFound = new FileNotFoundException("f");
    UnknownHostException unknownHost = new UnknownHostException("h");

    action.handleError("not found", notFound);
    action.handleError("unknown host", unknownHost);

    assertSame(notFound, assertStatus(Status.WARN, "not found").getThrowable());
    assertSame(unknownHost, assertStatus(Status.WARN, "unknown host").getThrowable());
  }

  @Test
  public void otherErrorsAreReportedAsErrors() {
    IOException ioException = new IOException("io");

    action.handleError("io failure", ioException);
    action.handleError("no exception", null);

    assertSame(ioException, assertStatus(Status.ERROR, "io failure").getThrowable());
    assertNull(assertStatus(Status.ERROR, "no exception").getThrowable());
  }

  @Test
  public void closeClosesTheStream() {
    CloseTrackingInputStream in = new CloseTrackingInputStream(null);

    action.close(in);

    assertTrue(in.closed);
  }

  @Test
  public void closeIgnoresNullAndFailingStreams() {
    CloseTrackingInputStream failing = new CloseTrackingInputStream(new IOException("cannot close"));

    action.close(null);
    action.close(failing);

    assertTrue(failing.closed);
    assertNoStatus();
  }

  @Test
  public void endChangesNothing() throws ActionException {
    ic.pushObject("top");

    action.end(ic, "include");

    assertEquals("top", ic.peekObject());
    assertTrue(action.includedUrls.isEmpty());
    assertNoStatus();
  }

  private Status assertStatus(int level, String message) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    for (Status s : statuses) {
      if (s.getLevel() == level && message.equals(s.getMessage())) {
        return s;
      }
    }
    throw new AssertionError("no status of level " + level + " with message [" + message + "] in " + statuses);
  }

  private void assertNoStatus() {
    assertEquals(Collections.emptyList(), context.getStatusManager().getCopyOfStatusList());
  }

  static class RecordingIncludeAction extends AbstractIncludeAction {
    final List<URL> includedUrls = new ArrayList<URL>();
    JoranException failure;

    @Override
    protected void processInclude(InterpretationContext ic, URL url) throws JoranException {
      includedUrls.add(url);
      if (failure != null) {
        throw failure;
      }
    }
  }

  static class CloseTrackingInputStream extends ByteArrayInputStream {
    final IOException closeFailure;
    boolean closed;

    CloseTrackingInputStream(IOException closeFailure) {
      super(new byte[0]);
      this.closeFailure = closeFailure;
    }

    @Override
    public void close() throws IOException {
      closed = true;
      if (closeFailure != null) {
        throw closeFailure;
      }
    }
  }
}
