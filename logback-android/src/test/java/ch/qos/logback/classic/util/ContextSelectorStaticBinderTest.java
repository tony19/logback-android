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
package ch.qos.logback.classic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.selector.DefaultContextSelector;

public class ContextSelectorStaticBinderTest {

  private final ContextSelectorStaticBinder binder = new ContextSelectorStaticBinder();
  private final LoggerContext defaultContext = new LoggerContext();
  private final Object key = new Object();
  private String savedSelectorProperty;

  @Before
  public void setUp() {
    savedSelectorProperty = System.getProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR);
    System.clearProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR);
  }

  @After
  public void tearDown() {
    if (savedSelectorProperty == null) {
      System.clearProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR);
    } else {
      System.setProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR, savedSelectorProperty);
    }
  }

  @Test
  public void singletonIsShared() {
    assertSame(ContextSelectorStaticBinder.getSingleton(), ContextSelectorStaticBinder.getSingleton());
  }

  @Test
  public void selectorIsNullBeforeInit() {
    assertNull(binder.getContextSelector());
  }

  @Test
  public void defaultSelectorServesTheDefaultContextWhenNoSelectorIsConfigured() throws Exception {
    binder.init(defaultContext, key);

    ContextSelector selector = binder.getContextSelector();
    assertTrue(selector instanceof DefaultContextSelector);
    assertSame(defaultContext, selector.getLoggerContext());
  }

  @Test
  public void initAgainWithTheSameKeyIsAllowed() throws Exception {
    binder.init(defaultContext, key);
    LoggerContext otherContext = new LoggerContext();

    binder.init(otherContext, key);

    assertSame(otherContext, binder.getContextSelector().getLoggerContext());
  }

  @Test
  public void initWithAnotherKeyIsRejected() throws Exception {
    binder.init(defaultContext, key);
    ContextSelector selector = binder.getContextSelector();

    IllegalAccessException e = assertThrows(IllegalAccessException.class,
        () -> binder.init(new LoggerContext(), new Object()));

    assertEquals("Only certain classes can access this method.", e.getMessage());
    assertSame(selector, binder.getContextSelector());
  }

  @Test
  public void jndiSelectorIsNotSupported() {
    System.setProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR, "JNDI");

    RuntimeException e = assertThrows(RuntimeException.class, () -> binder.init(defaultContext, key));

    assertEquals("JNDI not supported", e.getMessage());
    assertNull(binder.getContextSelector());
  }

  @Test
  public void configuredSelectorClassIsInstantiatedWithTheDefaultContext() throws Exception {
    System.setProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR, StubContextSelector.class.getName());

    binder.init(defaultContext, key);

    ContextSelector selector = binder.getContextSelector();
    assertEquals(StubContextSelector.class, selector.getClass());
    assertSame(defaultContext, selector.getLoggerContext());
  }

  @Test
  public void unknownSelectorClassFails() {
    System.setProperty(ClassicConstants.LOGBACK_CONTEXT_SELECTOR, "com.example.NoSuchContextSelector");

    assertThrows(ClassNotFoundException.class, () -> binder.init(defaultContext, key));
    assertNull(binder.getContextSelector());
  }

  @Test
  public void selectorClassWithoutLoggerContextConstructorFails() {
    assertThrows(NoSuchMethodException.class,
        () -> ContextSelectorStaticBinder.dynamicalContextSelector(defaultContext, Object.class.getName()));
  }

  /** A context selector with the constructor the binder requires. */
  public static class StubContextSelector implements ContextSelector {
    private final LoggerContext defaultContext;

    public StubContextSelector(LoggerContext defaultContext) {
      this.defaultContext = defaultContext;
    }

    @Override
    public LoggerContext getLoggerContext() {
      return defaultContext;
    }

    @Override
    public LoggerContext getLoggerContext(String name) {
      return defaultContext;
    }

    @Override
    public LoggerContext getDefaultLoggerContext() {
      return defaultContext;
    }

    @Override
    public LoggerContext detachLoggerContext(String loggerContextName) {
      return null;
    }

    @Override
    public List<String> getContextNames() {
      return Collections.singletonList(defaultContext.getName());
    }
  }
}
