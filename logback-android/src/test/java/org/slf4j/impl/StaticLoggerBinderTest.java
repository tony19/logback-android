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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.slf4j.ILoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.NopStatusListener;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Exercises fresh {@link StaticLoggerBinder} instances (built through the private
 * constructor) so that the JVM-wide {@link StaticLoggerBinder#getSingleton() singleton}
 * and {@link ContextSelectorStaticBinder} are left untouched.
 */
public class StaticLoggerBinderTest {

  /** What the (mocked) auto-configuration does to the binder's default context. */
  interface AutoConfig {
    void run(LoggerContext defaultContext) throws Exception;
  }

  private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
  private final ByteArrayOutputStream statusOutput = new ByteArrayOutputStream();
  private PrintStream originalStderr;
  private PrintStream originalStatusPrintStream;

  private final ContextSelectorStaticBinder selectorBinder = mock(ContextSelectorStaticBinder.class);
  private final ContextSelector selector = mock(ContextSelector.class);
  private final LoggerContext selectedContext = new LoggerContext();

  @Before
  public void setUp() throws Exception {
    // initialize the class (and its singleton) before any static/construction mocking
    StaticLoggerBinder.getSingleton();
    originalStderr = System.err;
    System.setErr(new PrintStream(stderr, true, "UTF-8"));
    originalStatusPrintStream = statusPrinterStream();
    StatusPrinter.setPrintStream(new PrintStream(statusOutput, true, "UTF-8"));
    when(selector.getLoggerContext()).thenReturn(selectedContext);
  }

  @After
  public void tearDown() {
    System.setErr(originalStderr);
    StatusPrinter.setPrintStream(originalStatusPrintStream);
  }

  @Test
  public void singletonIsTheSameInstanceOnEveryCall() {
    StaticLoggerBinder singleton = StaticLoggerBinder.getSingleton();

    assertNotNull(singleton);
    assertSame(singleton, StaticLoggerBinder.getSingleton());
  }

  @Test
  public void requestsTheSameApiVersionAsTheServiceProvider() throws Exception {
    assertEquals("2.0.7", newBinder().getRequestedApiVersion());
  }

  @Test
  public void markerFactoryIsTheStaticMarkerBindersFactory() throws Exception {
    assertSame(StaticMarkerBinder.getSingleton().getMarkerFactory(), newBinder().getMarkerFactory());
  }

  @Test
  public void mdcAdapterIsALogbackMdcAdapter() throws Exception {
    assertTrue(newBinder().getMDCAdapter() instanceof LogbackMDCAdapter);
  }

  @Test
  public void loggerFactoryClassNameIsTheContextSelectorBinder() throws Exception {
    assertEquals("ch.qos.logback.classic.util.ContextSelectorStaticBinder",
        newBinder().getLoggerFactoryClassStr());
  }

  @Test
  public void uninitializedBinderHandsOutItsOwnDefaultContext() throws Exception {
    StaticLoggerBinder binder = newBinder();

    ILoggerFactory factory = binder.getLoggerFactory();

    assertTrue(factory instanceof LoggerContext);
    assertEquals(CoreConstants.DEFAULT_CONTEXT_NAME, ((LoggerContext) factory).getName());
    assertSame(factory, binder.getLoggerFactory());
    assertNotSame(factory, newBinder().getLoggerFactory());
  }

  @Test
  public void initializedBinderHandsOutTheContextOfTheSelector() throws Exception {
    when(selectorBinder.getContextSelector()).thenReturn(selector);

    StaticLoggerBinder binder = newInitializedBinder(null);

    assertSame(selectedContext, binder.getLoggerFactory());
    ArgumentCaptor<LoggerContext> defaultContext = ArgumentCaptor.forClass(LoggerContext.class);
    verify(selectorBinder).init(defaultContext.capture(), notNull());
    assertEquals(CoreConstants.DEFAULT_CONTEXT_NAME, defaultContext.getValue().getName());
    // not assertEquals("", ...): the JVM and Mockito may print their own warnings to stderr
    String err = stderr.toString("UTF-8");
    assertFalse(err, err.contains("Failed to"));
  }

  @Test
  public void initializedBinderWithoutContextSelectorIsInIllegalState() throws Exception {
    when(selectorBinder.getContextSelector()).thenReturn(null);
    StaticLoggerBinder binder = newInitializedBinder(null);

    IllegalStateException e = assertThrows(IllegalStateException.class, binder::getLoggerFactory);

    assertEquals("contextSelector cannot be null. See also " + CoreConstants.CODES_URL + "#null_CS",
        e.getMessage());
  }

  @Test
  public void autoConfigurationFailureIsReportedAndBinderStillInitializes() throws Exception {
    when(selectorBinder.getContextSelector()).thenReturn(selector);

    StaticLoggerBinder binder = newInitializedBinder(context -> {
      throw new JoranException("broken config");
    });

    String err = stderr.toString("UTF-8");
    assertTrue(err, err.contains("Failed to auto configure default logger context"));
    assertTrue(err, err.contains("broken config"));
    assertSame(selectedContext, binder.getLoggerFactory());
  }

  @Test
  public void contextSelectorFailureIsReportedAndBinderFallsBackToItsDefaultContext() throws Exception {
    when(selectorBinder.getContextSelector()).thenReturn(selector);
    doThrow(new ClassNotFoundException("com.example.NoSuchSelector"))
        .when(selectorBinder).init(any(LoggerContext.class), any());

    StaticLoggerBinder binder = newInitializedBinder(null);

    String err = stderr.toString("UTF-8");
    assertTrue(err, err.contains("Failed to instantiate [" + LoggerContext.class.getName() + "]"));
    assertTrue(err, err.contains("com.example.NoSuchSelector"));
    ILoggerFactory factory = binder.getLoggerFactory();
    assertNotSame(selectedContext, factory);
    assertEquals(CoreConstants.DEFAULT_CONTEXT_NAME, ((LoggerContext) factory).getName());
    verify(selectorBinder, never()).getContextSelector();
  }

  @Test
  public void configurationErrorsArePrintedWhenNoStatusListenerIsRegistered() throws Exception {
    newInitializedBinder(context ->
        context.getStatusManager().add(new ErrorStatus("configuration went wrong", context)));

    String printed = statusOutput.toString("UTF-8");
    assertTrue(printed, printed.contains("configuration went wrong"));
  }

  @Test
  public void configurationErrorsAreNotPrintedWhenAStatusListenerIsRegistered() throws Exception {
    newInitializedBinder(context -> {
      context.getStatusManager().add(new NopStatusListener());
      context.getStatusManager().add(new ErrorStatus("configuration went wrong", context));
    });

    assertEquals("", statusOutput.toString("UTF-8"));
  }

  private static StaticLoggerBinder newBinder() throws Exception {
    Constructor<StaticLoggerBinder> constructor = StaticLoggerBinder.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  /**
   * Builds a binder bound to {@link #selectorBinder} and initializes it with a
   * {@link ContextInitializer} whose auto-configuration does nothing, or runs
   * {@code autoConfig} on the binder's default context when given.
   */
  private StaticLoggerBinder newInitializedBinder(final AutoConfig autoConfig) throws Exception {
    try (MockedStatic<ContextSelectorStaticBinder> binders = mockStatic(ContextSelectorStaticBinder.class);
         MockedConstruction<ContextInitializer> ignored = mockConstruction(ContextInitializer.class,
             (initializer, ctx) -> {
               final LoggerContext defaultContext = (LoggerContext) ctx.arguments().get(0);
               doAnswer(invocation -> {
                 if (autoConfig != null) {
                   autoConfig.run(defaultContext);
                 }
                 return null;
               }).when(initializer).autoConfig();
             })) {
      binders.when(ContextSelectorStaticBinder::getSingleton).thenReturn(selectorBinder);
      StaticLoggerBinder binder = newBinder();
      binder.initialize();
      return binder;
    }
  }

  private static PrintStream statusPrinterStream() throws Exception {
    Field ps = StatusPrinter.class.getDeclaredField("ps");
    ps.setAccessible(true);
    return (PrintStream) ps.get(null);
  }
}
