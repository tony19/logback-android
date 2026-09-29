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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockConstruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.function.ThrowingRunnable;
import org.junit.rules.ExternalResource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Runs the command-line entry points of {@link SimpleSocketServer} and
 * {@link SimpleSSLSocketServer} on the plain JVM, and restores the default
 * logger context that they configure.
 * <p>
 * Parsing a configuration file needs Android's XML pull parser, so the
 * {@link JoranConfigurator} that {@code main} creates is a mock. Configuring
 * it from {@link #CONFIG_FILE} stands in for that file: it attaches, to the
 * root logger of the context, an appender that records the events logged and
 * the threads that logged them.
 */
final class SimpleSocketServerMainRule extends ExternalResource {

  /** A port out of the TCP range: a server started on it fails to listen and ends. */
  static final String OUT_OF_RANGE_PORT = "70000";
  static final String CONFIG_FILE = "simple-socket-server.xml";

  private static final long TIMEOUT_MILLIS = 10000;

  private final LoggerContext defaultContext = (LoggerContext) LoggerFactory.getILoggerFactory();
  private final BlockingQueue<Logged> records = new LinkedBlockingQueue<Logged>();
  private final List<ILoggingEvent> seen = new ArrayList<ILoggingEvent>();
  private List<JoranConfigurator> configurators = Collections.emptyList();
  private boolean ran;

  LoggerContext defaultContext() {
    return defaultContext;
  }

  /** Runs the given entry point, with every {@link JoranConfigurator} it creates mocked. */
  void run(ThrowingRunnable main) throws Throwable {
    ran = true;
    try (MockedConstruction<JoranConfigurator> mocked = mockConstruction(JoranConfigurator.class,
        (configurator, context) -> doAnswer(invocation -> {
          Recorder recorder = new Recorder();
          recorder.setContext(defaultContext);
          recorder.start();
          defaultContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(recorder);
          return null;
        }).when(configurator).doConfigure(CONFIG_FILE))) {
      main.run();
      configurators = new ArrayList<JoranConfigurator>(mocked.constructed());
    }
  }

  /** Asserts that the entry point configured the default context from the given file. */
  void assertConfiguredDefaultContextFrom(String configFile) throws JoranException {
    assertEquals(1, configurators.size());
    JoranConfigurator configurator = configurators.get(0);
    InOrder inOrder = inOrder(configurator);
    inOrder.verify(configurator).setContext(defaultContext);
    inOrder.verify(configurator).doConfigure(configFile);
  }

  /**
   * Waits for the server to log that it failed, then for the thread that
   * logged it to end; returns the failure.
   */
  ILoggingEvent awaitServerFailure() throws InterruptedException {
    Logged next;
    while ((next = records.poll(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) != null) {
      seen.add(next.event);
      if ("Unexpected failure in run method".equals(next.event.getFormattedMessage())) {
        next.thread.join(TIMEOUT_MILLIS);
        assertFalse("the server thread is still running", next.thread.isAlive());
        return next.event;
      }
    }
    throw new AssertionError("the server did not report a failure; it logged " + seen);
  }

  /** Whether the given message was logged before the failure. */
  boolean logged(String message) {
    for (ILoggingEvent event : seen) {
      if (message.equals(event.getFormattedMessage())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void after() {
    if (!ran) {
      return;
    }
    // undo what the entry point configured into the default context
    defaultContext.reset();
    try {
      new ContextInitializer(defaultContext).autoConfig();
    } catch (JoranException e) {
      throw new IllegalStateException(e);
    }
  }

  /** An event, and the thread that logged it. */
  private static final class Logged {
    final ILoggingEvent event;
    final Thread thread;

    Logged(ILoggingEvent event, Thread thread) {
      this.event = event;
      this.thread = thread;
    }
  }

  /** Records the events it receives, and the threads that logged them. */
  private final class Recorder extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent event) {
      // resolve the thread name now, on the logging thread
      event.prepareForDeferredProcessing();
      records.offer(new Logged(event, Thread.currentThread()));
    }
  }
}
