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
package ch.qos.logback.classic.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;

/**
 * Plain-JVM tests (no Robolectric) of {@link ConditionalIncludeAction}: how it
 * reports include failures and when it skips an include. The end-to-end
 * behavior within {@code <findInclude>} is in
 * {@link ConditionalIncludeActionTest}.
 */
public class ConditionalIncludeActionJvmTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private final Context context = new ContextBase();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final ConditionalIncludeAction action = new ConditionalIncludeAction();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void missingFileIsReportedAsInfo() {
    action.handleError("missing", new FileNotFoundException("x.xml"));
    assertOnlyStatus(Status.INFO, "missing", null);
  }

  @Test
  public void unknownHostIsReportedAsInfo() {
    action.handleError("unknown host", new UnknownHostException("nowhere"));
    assertOnlyStatus(Status.INFO, "unknown host", null);
  }

  @Test
  public void errorWithoutExceptionIsReportedAsInfo() {
    action.handleError("no attribute", null);
    assertOnlyStatus(Status.INFO, "no attribute", null);
  }

  @Test
  public void otherFailuresAreReportedAsWarningWithTheirCause() {
    IOException cause = new IOException("disk on fire");
    action.handleError("cannot read", cause);
    assertOnlyStatus(Status.WARN, "cannot read", cause);
  }

  @Test
  public void includeIsSkippedOnceAPathWasFound() throws Exception {
    ConditionalIncludeAction.State found = new ConditionalIncludeAction.State();
    found.setUrl(URI.create("file:/found.xml").toURL());
    ic.pushObject(found);

    action.begin(ic, "include", fileAttribute(tmp.newFile("other.xml")));

    assertEquals(1, ic.getObjectStack().size());
    assertSame(found, ic.peekObject());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void includeIsSearchedWhenTheStateOnTheStackHasNoPath() throws Exception {
    ConditionalIncludeAction.State notFound = new ConditionalIncludeAction.State();
    ic.pushObject(notFound);
    File file = tmp.newFile("included.xml");

    action.begin(ic, "include", fileAttribute(file));

    assertEquals(2, ic.getObjectStack().size());
    ConditionalIncludeAction.State state = (ConditionalIncludeAction.State) ic.peekObject();
    assertEquals(file.toURI().toURL(), state.getUrl());
    assertNull(notFound.getUrl());
  }

  @Test
  public void foundPathIsPushedOntoAnEmptyStack() throws Exception {
    File file = tmp.newFile("included.xml");

    action.begin(ic, "include", fileAttribute(file));

    assertEquals(1, ic.getObjectStack().size());
    assertEquals(file.toURI().toURL(), ((ConditionalIncludeAction.State) ic.peekObject()).getUrl());
  }

  @Test
  public void missingPathPushesNothing() throws Exception {
    action.begin(ic, "include", fileAttribute(new File(tmp.getRoot(), "missing.xml")));

    assertTrue(ic.isEmpty());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.INFO, statuses.get(0).getLevel());
    assertTrue(statuses.get(0).getMessage().startsWith("File does not exist ["));
  }

  @Test
  public void includeIsSearchedWhenTheStackHoldsNoState() throws Exception {
    ic.pushObject("not a state");
    File file = tmp.newFile("included.xml");

    action.begin(ic, "include", fileAttribute(file));

    assertEquals(2, ic.getObjectStack().size());
    assertEquals(file.toURI().toURL(), ((ConditionalIncludeAction.State) ic.peekObject()).getUrl());
  }

  private static AttributesImpl fileAttribute(File file) {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "file", "file", "CDATA", file.getAbsolutePath());
    return attributes;
  }

  private void assertOnlyStatus(int level, String message, Throwable throwable) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 1, statuses.size());
    Status status = statuses.get(0);
    assertEquals(level, status.getLevel());
    assertEquals(message, status.getMessage());
    assertSame(throwable, status.getThrowable());
  }
}
