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
package ch.qos.logback.classic.spi;

import static junit.framework.Assert.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.BasicContextListener.UpdateType;

public class ContextListenerTest {

  LoggerContext context;
  BasicContextListener listener;

  @Before
  public void setUp() throws Exception {
    context = new LoggerContext();
    listener = new BasicContextListener();
    context.addListener(listener);
  }

  @Test
  public void testNotifyOnReset() {
    context.reset();
    assertEquals(UpdateType.RESET, listener.updateType);
    assertEquals(listener.context, context);
  }

  @Test
  public void testNotifyOnStopResistant() {
    listener.setResetResistant(true);
    context.stop();
    assertEquals(UpdateType.STOP, listener.updateType);
    assertEquals(listener.context, context);
  }

  @Test
  public void testNotifyOnStopNotResistant() {
    context.stop();
    assertEquals(UpdateType.RESET, listener.updateType);
    assertEquals(listener.context, context);
  }

  @Test
  public void testNotifyOnStart() {
    context.start();
    assertEquals(UpdateType.START, listener.updateType);
    assertEquals(listener.context, context);
  }

  void checkLevelChange(String loggerName, Level level) {
    Logger logger = context.getLogger(loggerName);
    logger.setLevel(level);

    assertEquals(UpdateType.LEVEL_CHANGE, listener.updateType);
    assertEquals(listener.logger, logger);
    assertEquals(listener.level, level);

  }

  @Test
  public void testLevelChange() {
    checkLevelChange("a", Level.INFO);
    checkLevelChange("a.b", Level.ERROR);
    checkLevelChange("a.b.c", Level.DEBUG);
  }
}
