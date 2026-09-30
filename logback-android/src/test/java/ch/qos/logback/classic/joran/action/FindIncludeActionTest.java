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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;

/**
 * Tests {@link FindIncludeAction#end}, which includes the path found by the
 * nested {@link ConditionalIncludeAction}s, if any. The include itself is
 * stubbed: parsing it needs Android's XML parser (see
 * {@link ConditionalIncludeActionTest} for the end-to-end behavior).
 */
public class FindIncludeActionTest {

  private final Context context = new ContextBase();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final List<URL> included = new ArrayList<URL>();
  private JoranException includeFailure;

  private final FindIncludeAction action = new FindIncludeAction() {
    @Override
    protected void processInclude(InterpretationContext ic, URL url) throws JoranException {
      included.add(url);
      if (includeFailure != null) {
        throw includeFailure;
      }
    }
  };

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void beginLeavesTheStackAlone() throws Exception {
    action.begin(ic, "findInclude", new AttributesImpl());
    assertTrue(ic.isEmpty());
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void endWithEmptyStackIncludesNothing() throws Exception {
    action.end(ic, "findInclude");

    assertTrue(included.isEmpty());
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void endLeavesAForeignObjectOnTheStack() throws Exception {
    ic.pushObject("not a state");

    action.end(ic, "findInclude");

    assertEquals("not a state", ic.peekObject());
    assertTrue(included.isEmpty());
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void endReportsWhenNoPathWasFound() throws Exception {
    ic.pushObject(new ConditionalIncludeAction.State());

    action.end(ic, "findInclude");

    assertTrue(ic.isEmpty());
    assertTrue(included.isEmpty());
    assertOnlyStatuses(new int[] { Status.INFO }, "No paths found from includes");
  }

  @Test
  public void endIncludesTheFoundPath() throws Exception {
    URL url = URI.create("file:/found.xml").toURL();
    pushFoundPath(url);

    action.end(ic, "findInclude");

    assertTrue(ic.isEmpty());
    assertEquals(1, included.size());
    assertSame(url, included.get(0));
    assertOnlyStatuses(new int[] { Status.INFO }, "Path found [file:/found.xml]");
  }

  @Test
  public void endReportsFailureToIncludeTheFoundPath() throws Exception {
    URL url = URI.create("file:/found.xml").toURL();
    pushFoundPath(url);
    includeFailure = new JoranException("cannot include");

    action.end(ic, "findInclude");

    assertTrue(ic.isEmpty());
    assertEquals(1, included.size());
    assertOnlyStatuses(new int[] { Status.INFO, Status.ERROR },
        "Path found [file:/found.xml]", "Failed to process include [file:/found.xml]");
    assertSame(includeFailure, statuses().get(1).getThrowable());
  }

  @Test
  public void recorderIsBoundToTheActionsContext() {
    assertSame(context, action.createRecorder(null, null).getContext());
  }

  private void pushFoundPath(URL url) {
    ConditionalIncludeAction.State state = new ConditionalIncludeAction.State();
    state.setUrl(url);
    ic.pushObject(state);
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  private void assertOnlyStatuses(int[] levels, String... messages) {
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), messages.length, statuses.size());
    for (int i = 0; i < messages.length; i++) {
      assertEquals(levels[i], statuses.get(i).getLevel());
      assertEquals(messages[i], statuses.get(i).getMessage());
    }
  }
}
