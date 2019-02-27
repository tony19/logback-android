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

import static junit.framework.Assert.assertEquals;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.core.status.StatusChecker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactoryFriend;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.StatusPrinter;

@RunWith(RobolectricTestRunner.class)
public class RecursiveInitializationTest {

  int diff = RandomUtil.getPositiveInt();

  @Before
  public void setUp() throws Exception {
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY,
            ClassicTestConstants.RESOURCES_PREFIX + "recursiveInit.xml");
    StaticLoggerBinderFriend.reset();
    LoggerFactoryFriend.reset();

  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
  }

  @Test
  public void recursiveLogbackInitialization() {
    Logger logger = LoggerFactory.getLogger("RecursiveInitializationTest"
        + diff);
    logger.info("RecursiveInitializationTest");

    LoggerContext loggerContext = (LoggerContext) LoggerFactory
        .getILoggerFactory();
    StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    StatusChecker statusChecker = new StatusChecker(loggerContext);
    assertEquals("Was expecting no errors", Status.WARN, statusChecker.getHighestLevel(0));
  }

}
