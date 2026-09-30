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
package ch.qos.logback.classic.turbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.RandomUtil;

public class DynamicThresholdFilterTest {

  int diff = RandomUtil.getPositiveInt();
  String key = "userId" + diff;

  ContextBase context = new ContextBase();
  StatusChecker statusChecker = new StatusChecker(context);
  DynamicThresholdFilter filter = new DynamicThresholdFilter();

  @Before
  public void setUp() {
    filter.setContext(context);
    MDC.remove(key);
  }

  @After
  public void tearDown() {
    MDC.remove(key);
  }

  @Test
  public void propertiesHaveDefaultsAndCanBeSet() {
    assertNull(filter.getKey());
    assertEquals(Level.ERROR, filter.getDefaultThreshold());
    assertEquals(FilterReply.NEUTRAL, filter.getOnHigherOrEqual());
    assertEquals(FilterReply.DENY, filter.getOnLower());

    filter.setKey(key);
    filter.setDefaultThreshold(Level.WARN);
    filter.setOnHigherOrEqual(FilterReply.ACCEPT);
    filter.setOnLower(FilterReply.NEUTRAL);

    assertEquals(key, filter.getKey());
    assertEquals(Level.WARN, filter.getDefaultThreshold());
    assertEquals(FilterReply.ACCEPT, filter.getOnHigherOrEqual());
    assertEquals(FilterReply.NEUTRAL, filter.getOnLower());
  }

  @Test
  public void startWithoutKeyReportsAnErrorButStillStarts() {
    filter.start();

    assertTrue(filter.isStarted());
    statusChecker.assertContainsMatch(Status.ERROR, Pattern.quote("No key name was specified"));
  }

  @Test
  public void startWithKeyReportsNoError() {
    filter.setKey(key);
    filter.start();

    assertTrue(filter.isStarted());
    statusChecker.assertIsErrorFree();
  }

  @Test
  public void decideIsNeutralWhenNotStarted() {
    filter.setKey(key);
    filter.setOnHigherOrEqual(FilterReply.ACCEPT);
    filter.addMDCValueLevelPair(pair("user1", Level.DEBUG));
    MDC.put(key, "user1");

    assertEquals(FilterReply.NEUTRAL, decide(Level.ERROR));
    assertEquals(FilterReply.NEUTRAL, decide(Level.TRACE));
  }

  @Test
  public void decideAppliesTheDefaultThresholdWhenTheKeyIsNotInMdc() {
    startWithUser1AtDebug();

    assertEquals(FilterReply.DENY, decide(Level.WARN));
    assertEquals(FilterReply.ACCEPT, decide(Level.ERROR));
  }

  @Test
  public void decideAppliesTheDefaultThresholdToAnUnmappedMdcValue() {
    startWithUser1AtDebug();
    MDC.put(key, "user2");

    assertEquals(FilterReply.DENY, decide(Level.WARN));
    assertEquals(FilterReply.ACCEPT, decide(Level.ERROR));
  }

  @Test
  public void decideAppliesTheLevelMappedToTheMdcValue() {
    startWithUser1AtDebug();
    MDC.put(key, "user1");

    assertEquals(FilterReply.DENY, decide(Level.TRACE));
    assertEquals(FilterReply.ACCEPT, decide(Level.DEBUG));
    assertEquals(FilterReply.ACCEPT, decide(Level.INFO));
  }

  @Test
  public void duplicateMdcValueIsReportedAndTheFirstLevelIsKept() {
    filter.addMDCValueLevelPair(pair("user1", Level.DEBUG));
    filter.addMDCValueLevelPair(pair("user1", Level.ERROR));

    statusChecker.assertContainsMatch(Status.ERROR, Pattern.quote("user1 has been already set"));
    filter.setKey(key);
    filter.setOnHigherOrEqual(FilterReply.ACCEPT);
    filter.start();
    MDC.put(key, "user1");
    assertEquals(FilterReply.ACCEPT, decide(Level.DEBUG));
  }

  private void startWithUser1AtDebug() {
    filter.setKey(key);
    filter.setOnHigherOrEqual(FilterReply.ACCEPT);
    filter.setOnLower(FilterReply.DENY);
    filter.addMDCValueLevelPair(pair("user1", Level.DEBUG));
    filter.start();
  }

  private FilterReply decide(Level level) {
    return filter.decide(null, null, level, "msg", null, null);
  }

  private static MDCValueLevelPair pair(String value, Level level) {
    MDCValueLevelPair pair = new MDCValueLevelPair();
    pair.setValue(value);
    pair.setLevel(level);
    return pair;
  }
}
