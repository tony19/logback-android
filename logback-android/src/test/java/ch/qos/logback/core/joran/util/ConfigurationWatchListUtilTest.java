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
package ch.qos.logback.core.joran.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.status.Status;

public class ConfigurationWatchListUtilTest {

  // the files need not exist: the watch list only records them
  static final String MAIN_PATH = "/logback-test/main.xml";
  static final String INCLUDED_PATH = "/logback-test/included.xml";

  Context context = new ContextBase();

  private static URL fileURL(String path) throws Exception {
    return new URI("file", null, path, null).toURL();
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  @Test
  public void nullContextHasNoWatchList() throws Exception {
    // must not throw
    ConfigurationWatchListUtil.setMainWatchURL(null, fileURL(MAIN_PATH));
    assertNull(ConfigurationWatchListUtil.getConfigurationWatchList(null));
    assertNull(ConfigurationWatchListUtil.getMainWatchURL(null));
  }

  @Test
  public void setMainWatchURLCreatesAndRegistersWatchList() throws Exception {
    URL main = fileURL(MAIN_PATH);
    ConfigurationWatchListUtil.setMainWatchURL(context, main);

    ConfigurationWatchList cwl = ConfigurationWatchListUtil.getConfigurationWatchList(context);
    assertSame(cwl, context.getObject(CoreConstants.CONFIGURATION_WATCH_LIST));
    assertSame(context, cwl.getContext());
    assertEquals(main, ConfigurationWatchListUtil.getMainWatchURL(context));
    assertEquals(Collections.singletonList(new File(MAIN_PATH)), cwl.getCopyOfFileWatchList());
  }

  @Test
  public void setMainWatchURLClearsExistingWatchList() throws Exception {
    ConfigurationWatchList cwl = new ConfigurationWatchList();
    ConfigurationWatchListUtil.registerConfigurationWatchList(context, cwl);
    ConfigurationWatchListUtil.addToWatchList(context, fileURL(INCLUDED_PATH));

    URL main = fileURL(MAIN_PATH);
    ConfigurationWatchListUtil.setMainWatchURL(context, main);

    assertSame(cwl, ConfigurationWatchListUtil.getConfigurationWatchList(context));
    assertEquals(main, cwl.getMainURL());
    assertEquals(Collections.singletonList(new File(MAIN_PATH)), cwl.getCopyOfFileWatchList());
  }

  @Test
  public void getMainWatchURLIsNullWithoutWatchList() {
    assertNull(ConfigurationWatchListUtil.getMainWatchURL(context));
  }

  @Test
  public void addToWatchListAddsFileAndReportsIt() throws Exception {
    ConfigurationWatchList cwl = new ConfigurationWatchList();
    ConfigurationWatchListUtil.registerConfigurationWatchList(context, cwl);
    URL included = fileURL(INCLUDED_PATH);

    ConfigurationWatchListUtil.addToWatchList(context, included);

    assertEquals(Collections.singletonList(new File(INCLUDED_PATH)), cwl.getCopyOfFileWatchList());
    List<Status> list = statuses();
    assertEquals(1, list.size());
    assertEquals(Status.INFO, list.get(0).getLevel());
    assertEquals("Adding [" + included + "] to configuration watch list.", list.get(0).getMessage());
    assertSame(ConfigurationWatchListUtil.origin, list.get(0).getOrigin());
  }

  @Test
  public void addToWatchListWarnsWithoutWatchList() throws Exception {
    URL included = fileURL(INCLUDED_PATH);
    ConfigurationWatchListUtil.addToWatchList(context, included);

    List<Status> list = statuses();
    assertEquals(1, list.size());
    assertEquals(Status.WARN, list.get(0).getLevel());
    assertEquals("Null ConfigurationWatchList. Cannot add " + included, list.get(0).getMessage());
  }

  @Test
  public void addToWatchListWithNullContextPrintsToConsole() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream original = System.out;
    System.setOut(new PrintStream(out, true, "UTF-8"));
    try {
      ConfigurationWatchListUtil.addToWatchList(null, fileURL(INCLUDED_PATH));
    } finally {
      System.setOut(original);
    }
    String printed = out.toString("UTF-8");
    assertTrue(printed, printed.startsWith("Null context in " + ConfigurationWatchList.class.getName()));
  }

  @Test
  public void addToWatchListWithoutStatusManagerStillAddsFile() throws Exception {
    ConfigurationWatchList cwl = new ConfigurationWatchList();
    Context ctx = mock(Context.class);
    when(ctx.getObject(CoreConstants.CONFIGURATION_WATCH_LIST)).thenReturn(cwl);
    when(ctx.getStatusManager()).thenReturn(null);

    ConfigurationWatchListUtil.addToWatchList(ctx, fileURL(INCLUDED_PATH));

    verify(ctx).getStatusManager();
    assertEquals(Collections.singletonList(new File(INCLUDED_PATH)), cwl.getCopyOfFileWatchList());
  }
}
