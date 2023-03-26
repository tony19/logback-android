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

import static org.slf4j.impl.LoggerServiceProvider.REQUESTED_API_VERSION;

import ch.qos.logback.core.status.StatusUtil;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.LoggerFactoryBinder;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.concurrent.ConcurrentMap;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 *
 * The binding of {@link LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class StaticLoggerBinder implements SLF4JServiceProvider {

  final static String NULL_CS_URL = CoreConstants.CODES_URL + "#null_CS";

  /**
   * The unique instance of this class.
   */
  private static StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  private static Object KEY = new Object();

  static {
    SINGLETON.initialize();
  }

  private boolean initialized = false;
  private LoggerContext defaultLoggerContext = new LoggerContext();
  private final ContextSelectorStaticBinder contextSelectorBinder = ContextSelectorStaticBinder
      .getSingleton();

  private StaticLoggerBinder() {
    defaultLoggerContext.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
  }

  public static StaticLoggerBinder getSingleton() {
    return SINGLETON;
  }

  /**
   * Package access for testing purposes.
   */
  static void reset() {
    SINGLETON = new StaticLoggerBinder();
    SINGLETON.initialize();
  }

  @Override
  public void initialize() {
    try {
      try {
        new ContextInitializer(defaultLoggerContext).autoConfig();
      } catch (JoranException je) {
        Util.report("Failed to auto configure default logger context", je);
      }
      // logback-292
      if(!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
        StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext);
      }
      contextSelectorBinder.init(defaultLoggerContext, KEY);
      initialized = true;
    } catch (Exception t) { // see LOGBACK-1159
      Util.report("Failed to instantiate [" + LoggerContext.class.getName()
          + "]", t);
    }
  }

  public ILoggerFactory getLoggerFactory() {
    if (!initialized) {
      return defaultLoggerContext;
    }

    if (contextSelectorBinder.getContextSelector() == null) {
      throw new IllegalStateException(
          "contextSelector cannot be null. See also " + NULL_CS_URL);
    }
    return contextSelectorBinder.getContextSelector().getLoggerContext();
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return StaticMarkerBinder.SINGLETON.getMarkerFactory();
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return StaticMDCBinder.SINGLETON.getMDCA();
  }

  @Override
  public String getRequestedApiVersion() {
    return REQUESTED_API_VERSION;
  }

  public String getLoggerFactoryClassStr() {
    return contextSelectorBinder.getClass().getName();
  }
}
