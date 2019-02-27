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
package ch.qos.logback.classic.jul;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;


/**
 * Propagate level changes made to a logback logger into the equivalent logger in j.u.l.
 * @deprecated
 */
@Deprecated
public class LevelChangePropagator extends ContextAwareBase implements LoggerContextListener, LifeCycle {

  private Set<java.util.logging.Logger> julLoggerSet = new HashSet<java.util.logging.Logger>();
  boolean isStarted = false;
  boolean resetJUL = false;

  public void setResetJUL(boolean resetJUL) {
    this.resetJUL = resetJUL;
  }

  public boolean isResetResistant() {
    return false;
  }

  public void onStart(LoggerContext context) {
  }

  public void onReset(LoggerContext context) {
  }

  public void onStop(LoggerContext context) {
  }

  public void onLevelChange(Logger logger, Level level) {
    propagate(logger, level);
  }

  @SuppressWarnings("deprecation")
  private void propagate(Logger logger, Level level) {
    addInfo("Propagating " + level + " level on " + logger + " onto the JUL framework");
    java.util.logging.Logger julLogger = JULHelper.asJULLogger(logger);
    // prevent garbage collection of jul loggers whose level we set
    // see also  http://jira.qos.ch/browse/LBCLASSIC-256
    julLoggerSet.add(julLogger);
    java.util.logging.Level julLevel = JULHelper.asJULLevel(level);
    julLogger.setLevel(julLevel);
  }

  public void resetJULLevels() {
    LogManager lm = LogManager.getLogManager();

    Enumeration<String> e = lm.getLoggerNames();
    while (e.hasMoreElements()) {
      String loggerName = e.nextElement();
      java.util.logging.Logger julLogger = lm.getLogger(loggerName);
      if (JULHelper.isRegularNonRootLogger(julLogger) && julLogger.getLevel() != null) {
        addInfo("Setting level of jul logger [" + loggerName + "] to null");
        julLogger.setLevel(null);
      }
    }
  }

  private void propagateExistingLoggerLevels() {
    LoggerContext loggerContext = (LoggerContext) context;
    List<Logger> loggerList = loggerContext.getLoggerList();
    for (Logger l : loggerList) {
      if (l.getLevel() != null) {
        propagate(l, l.getLevel());
      }
    }
  }

  public void start() {
    if (resetJUL) {
      resetJULLevels();
    }
    propagateExistingLoggerLevels();

    isStarted = true;
  }

  public void stop() {
    isStarted = false;
  }

  public boolean isStarted() {
    return isStarted;
  }
}
