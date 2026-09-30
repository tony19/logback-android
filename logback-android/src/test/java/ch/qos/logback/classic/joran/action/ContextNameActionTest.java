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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;

public class ContextNameActionTest {

  private final LoggerContext context = new LoggerContext();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final ContextNameAction action = new ContextNameAction();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void bodyNamesTheContextAfterSubstitution() throws Exception {
    context.putProperty("appName", "myApp");

    action.begin(ic, "contextName", new AttributesImpl());
    action.body(ic, "${appName}");
    action.end(ic, "contextName");

    assertEquals("myApp", context.getName());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.INFO, statuses.get(0).getLevel());
    assertEquals("Setting logger context name as [myApp]", statuses.get(0).getMessage());
    assertTrue(ic.isEmpty());
  }

  @Test
  public void renamingAnAlreadyNamedContextIsReportedAsError() {
    context.setName("first");

    action.body(ic, "second");

    assertEquals("first", context.getName());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 2, statuses.size());
    assertEquals("Setting logger context name as [second]", statuses.get(0).getMessage());
    Status error = statuses.get(1);
    assertEquals(Status.ERROR, error.getLevel());
    assertEquals("Failed to rename context [first] as [second]", error.getMessage());
    assertTrue(error.getThrowable() instanceof IllegalStateException);
  }
}
