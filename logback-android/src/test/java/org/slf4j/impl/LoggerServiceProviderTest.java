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
package org.slf4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockConstruction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.slf4j.helpers.BasicMarkerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.NopStatusListener;
import ch.qos.logback.core.util.StatusPrinter;

public class LoggerServiceProviderTest {

  /** What the (mocked) auto-configuration does to the provider's default context. */
  interface AutoConfig {
    void run(LoggerContext defaultContext) throws Exception;
  }

  private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
  private final ByteArrayOutputStream statusOutput = new ByteArrayOutputStream();
  private PrintStream originalStderr;
  private PrintStream originalStatusPrintStream;

  private final LoggerServiceProvider provider = new LoggerServiceProvider();
  /** The contexts handed to each ContextInitializer created during {@link #initialize}. */
  private final List<LoggerContext> autoConfiguredContexts = new ArrayList<LoggerContext>();

  @Before
  public void setUp() throws Exception {
    originalStderr = System.err;
    System.setErr(new PrintStream(stderr, true, "UTF-8"));
    originalStatusPrintStream = statusPrinterStream();
    StatusPrinter.setPrintStream(new PrintStream(statusOutput, true, "UTF-8"));
  }

  @After
  public void tearDown() {
    System.setErr(originalStderr);
    StatusPrinter.setPrintStream(originalStatusPrintStream);
  }

  @Test
  public void requestsTheSlf4jApiVersionItIsCompiledAgainst() {
    assertEquals("2.0.7", provider.getRequestedApiVersion());
    assertEquals(LoggerServiceProvider.REQUESTED_API_VERSION, provider.getRequestedApiVersion());
  }

  @Test
  public void initializeAutoConfiguresAndStartsTheDefaultContextAndCreatesFactories() throws Exception {
    initialize(null);

    assertDefaultContextStartedAndFactoriesCreated();
    assertEquals(1, autoConfiguredContexts.size());
    assertSame(provider.getLoggerFactory(), autoConfiguredContexts.get(0));
    // not assertEquals("", ...): the JVM and Mockito may print their own warnings to stderr
    String err = stderr.toString("UTF-8");
    assertFalse(err, err.contains("Failed to"));
  }

  @Test
  public void autoConfigurationFailureIsReportedAndInitializationContinues() throws Exception {
    initialize(context -> {
      throw new JoranException("broken config");
    });

    String err = stderr.toString("UTF-8");
    assertTrue(err, err.contains("Failed to auto configure default logger context"));
    assertTrue(err, err.contains("broken config"));
    assertDefaultContextStartedAndFactoriesCreated();
  }

  @Test
  public void unexpectedExceptionDuringConfigurationIsReportedAndInitializationContinues()
      throws Exception {
    initialize(context -> {
      throw new IllegalStateException("unexpected");
    });

    String err = stderr.toString("UTF-8");
    assertTrue(err, err.contains("Failed to instantiate [" + LoggerContext.class.getName() + "]"));
    assertTrue(err, err.contains("unexpected"));
    assertDefaultContextStartedAndFactoriesCreated();
  }

  @Test
  public void configurationErrorsArePrintedWhenNoStatusListenerIsRegistered() throws Exception {
    initialize(context ->
        context.getStatusManager().add(new ErrorStatus("configuration went wrong", context)));

    String printed = statusOutput.toString("UTF-8");
    assertTrue(printed, printed.contains("configuration went wrong"));
  }

  @Test
  public void configurationErrorsAreNotPrintedWhenAStatusListenerIsRegistered() throws Exception {
    initialize(context -> {
      context.getStatusManager().add(new NopStatusListener());
      context.getStatusManager().add(new ErrorStatus("configuration went wrong", context));
    });

    assertEquals("", statusOutput.toString("UTF-8"));
  }

  /**
   * Initializes {@link #provider} with a {@link ContextInitializer} whose auto-configuration
   * does nothing, or runs {@code autoConfig} on the provider's default context when given.
   */
  private void initialize(final AutoConfig autoConfig) {
    try (MockedConstruction<ContextInitializer> ignored = mockConstruction(ContextInitializer.class,
        (initializer, ctx) -> {
          final LoggerContext defaultContext = (LoggerContext) ctx.arguments().get(0);
          autoConfiguredContexts.add(defaultContext);
          doAnswer(invocation -> {
            if (autoConfig != null) {
              autoConfig.run(defaultContext);
            }
            return null;
          }).when(initializer).autoConfig();
        })) {
      provider.initialize();
    }
  }

  private void assertDefaultContextStartedAndFactoriesCreated() {
    assertTrue(provider.getLoggerFactory() instanceof LoggerContext);
    LoggerContext context = (LoggerContext) provider.getLoggerFactory();
    assertEquals(CoreConstants.DEFAULT_CONTEXT_NAME, context.getName());
    assertTrue(context.isStarted());
    assertTrue(provider.getMarkerFactory() instanceof BasicMarkerFactory);
    assertTrue(provider.getMDCAdapter() instanceof LogbackMDCAdapter);
  }

  private static PrintStream statusPrinterStream() throws Exception {
    Field ps = StatusPrinter.class.getDeclaredField("ps");
    ps.setAccessible(true);
    return (PrintStream) ps.get(null);
  }
}
