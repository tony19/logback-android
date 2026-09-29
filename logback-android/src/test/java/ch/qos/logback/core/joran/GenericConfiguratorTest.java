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
package ch.qos.logback.core.joran;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockConstruction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.xmlpull.v1.sax2.Driver;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.Status;

/**
 * Plain-JVM tests of {@link GenericConfigurator}'s I/O error handling.
 * Configuring from real documents is covered by the Robolectric
 * {@link TrivialConfiguratorTest}.
 */
public class GenericConfiguratorTest {

  private final Context context = new ContextBase();
  private final TrivialConfigurator configurator =
      new TrivialConfigurator(new HashMap<ElementSelector, Action>());

  @Before
  public void setUp() {
    configurator.setContext(context);
  }

  private Status errorStatus(String message) {
    for (Status status : context.getStatusManager().getCopyOfStatusList()) {
      if (status.getLevel() == Status.ERROR && message.equals(status.getMessage())) {
        return status;
      }
    }
    throw new AssertionError("no error status [" + message + "] in "
        + context.getStatusManager().getCopyOfStatusList());
  }

  @Test
  public void urlThatCannotBeOpenedIsReportedButStillWatched() throws Exception {
    final IOException failure = new IOException("connection refused");
    URL url = new URL(null, "test:config.xml", new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL u) throws IOException {
        throw failure;
      }
    });

    JoranException e = assertThrows(JoranException.class, () -> configurator.doConfigure(url));

    assertEquals("Could not open URL [test:config.xml].", e.getMessage());
    assertSame(failure, e.getCause());
    assertSame(failure, errorStatus("Could not open URL [test:config.xml].").getThrowable());
    assertSame(url, ConfigurationWatchListUtil.getMainWatchURL(context));
  }

  @Test
  public void streamThatFailsToCloseIsReportedAfterConfiguring() throws Exception {
    final IOException failure = new IOException("close failed");
    InputStream in = new ByteArrayInputStream("<x/>".getBytes("UTF-8")) {
      @Override
      public void close() throws IOException {
        throw failure;
      }
    };

    // stand-in parser that reports no events: the configuration itself succeeds
    try (MockedConstruction<Driver> ignored = mockConstruction(Driver.class)) {
      JoranException e = assertThrows(JoranException.class, () -> configurator.doConfigure(in));

      assertEquals("Could not close the stream", e.getMessage());
      assertSame(failure, e.getCause());
    }
    assertSame(failure, errorStatus("Could not close the stream").getThrowable());
    List<?> safeConfiguration = configurator.recallSafeConfiguration();
    assertNotNull(safeConfiguration);
    assertTrue(safeConfiguration.isEmpty());
  }
}
