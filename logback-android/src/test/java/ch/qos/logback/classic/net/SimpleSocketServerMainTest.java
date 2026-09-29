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
package ch.qos.logback.classic.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.AppenderBase;

/**
 * Tests the command-line entry points of {@link SimpleSocketServer} and
 * {@link SimpleSSLSocketServer}. They configure the default context from an
 * XML file, which needs Android's XML pull parser, hence Robolectric.
 * <p>
 * The port given to {@code main} is out of the TCP range, so the server thread
 * that {@code main} starts logs its failure to listen and ends on its own.
 */
@RunWith(RobolectricTestRunner.class)
public class SimpleSocketServerMainTest {

  private static final String OUT_OF_RANGE_PORT = "70000";
  private static final long TIMEOUT_MILLIS = 10000;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private LoggerContext defaultContext;

  @Before
  public void setUp() {
    defaultContext = (LoggerContext) LoggerFactory.getILoggerFactory();
  }

  @After
  public void tearDown() throws Exception {
    // undo what main configured into the default context
    defaultContext.reset();
    new ContextInitializer(defaultContext).autoConfig();
  }

  @Test
  public void mainConfiguresTheDefaultContextAndStartsASocketServerOnThePort() throws Exception {
    SimpleSocketServer.main(new String[] {OUT_OF_RANGE_PORT, writeCapturingConfig()});

    CapturingAppender capture = CapturingAppender.from(defaultContext);
    assertNotNull("main did not apply the config file", capture);
    ILoggingEvent failure = capture.awaitMessage("Unexpected failure in run method");
    capture.joinLoggingThreads();

    assertNotNull(failure);
    assertEquals("Logback SimpleSocketServer (port " + OUT_OF_RANGE_PORT + ")", failure.getThreadName());
    assertEquals(IllegalArgumentException.class.getName(), failure.getThrowableProxy().getClassName());
    assertTrue(capture.hasMessage("Listening on port " + OUT_OF_RANGE_PORT));
  }

  @Test
  public void sslMainStartsAnSslSocketServer() throws Exception {
    SimpleSSLSocketServer.main(new String[] {OUT_OF_RANGE_PORT, writeCapturingConfig()});

    CapturingAppender capture = CapturingAppender.from(defaultContext);
    assertNotNull("main did not apply the config file", capture);
    ILoggingEvent failure = capture.awaitMessage("Unexpected failure in run method");
    capture.joinLoggingThreads();

    assertNotNull(failure);
    assertEquals("Logback SimpleSSLSocketServer (port " + OUT_OF_RANGE_PORT + ")", failure.getThreadName());
  }

  @Test
  public void configureLCResetsTheContextAndAppliesTheConfigFile() throws Exception {
    LoggerContext lc = new LoggerContext();
    Logger stale = lc.getLogger("stale");
    stale.setLevel(Level.ERROR);
    try {
      SimpleSocketServer.configureLC(lc, writeCapturingConfig());

      assertNull(stale.getLevel());
      assertNotNull(CapturingAppender.from(lc));
      assertEquals(Level.DEBUG, lc.getLogger(Logger.ROOT_LOGGER_NAME).getLevel());
    } finally {
      lc.stop();
    }
  }

  /**
   * Writes a configuration that sends every event of the context to a
   * {@link CapturingAppender} named {@code CAPTURE}.
   */
  private String writeCapturingConfig() throws IOException {
    File file = tmp.newFile("capture.xml");
    Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
    try {
      writer.write("<configuration>\n"
          + "  <appender name='CAPTURE' class='" + CapturingAppender.class.getName() + "'/>\n"
          + "  <root level='DEBUG'><appender-ref ref='CAPTURE'/></root>\n"
          + "</configuration>\n");
    } finally {
      writer.close();
    }
    return file.getAbsolutePath();
  }

  /**
   * An appender, configured by class name, that keeps the events it receives
   * and the threads that logged them.
   */
  public static class CapturingAppender extends AppenderBase<ILoggingEvent> {
    private final BlockingQueue<ILoggingEvent> events = new LinkedBlockingQueue<ILoggingEvent>();
    private final List<ILoggingEvent> seen = new ArrayList<ILoggingEvent>();
    private final List<Thread> threads = new ArrayList<Thread>();

    static CapturingAppender from(LoggerContext context) {
      return (CapturingAppender) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("CAPTURE");
    }

    @Override
    protected void append(ILoggingEvent event) {
      // resolve the thread name now, on the logging thread
      event.prepareForDeferredProcessing();
      synchronized (threads) {
        if (!threads.contains(Thread.currentThread())) {
          threads.add(Thread.currentThread());
        }
      }
      events.offer(event);
    }

    ILoggingEvent awaitMessage(String message) throws InterruptedException {
      ILoggingEvent event;
      while ((event = events.poll(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) != null) {
        seen.add(event);
        if (message.equals(event.getFormattedMessage())) {
          return event;
        }
      }
      return null;
    }

    boolean hasMessage(String message) {
      for (ILoggingEvent event : seen) {
        if (message.equals(event.getFormattedMessage())) {
          return true;
        }
      }
      return false;
    }

    void joinLoggingThreads() throws InterruptedException {
      List<Thread> copy;
      synchronized (threads) {
        copy = new ArrayList<Thread>(threads);
      }
      for (Thread thread : copy) {
        if (thread != Thread.currentThread()) {
          thread.join(TIMEOUT_MILLIS);
          assertFalse(thread.isAlive());
        }
      }
    }
  }
}
