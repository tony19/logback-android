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
package ch.qos.logback.core.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;

/**
 * Tests {@link TimestampAction}.
 */
public class TimestampActionTest {

  /** 2001-07-01T12:00:00.123Z: in 2001 in every time zone, and long before "now". */
  private static final long BIRTH_TIME = 993_988_800_123L;
  /** Year and milliseconds: the same for BIRTH_TIME in every time zone. */
  private static final String PATTERN = "yyyy.SSS";
  private static final String BIRTH_TIME_FORMATTED = "2001.123";

  private final Context context = new ContextBase() {
    @Override
    public long getBirthTime() {
      return BIRTH_TIME;
    }
  };
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final TimestampAction action = new TimestampAction();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void contextBirthIsUsedAsTimeReference() throws ActionException {
    action.begin(ic, "timestamp", timestampAttributes("birth", PATTERN, "ContextBirth", null));

    // the date is formatted in the US locale (Gregorian calendar, ASCII digits) whatever the default one
    assertEquals(BIRTH_TIME_FORMATTED, ic.getProperty("birth"));
    assertNull(context.getProperty("birth"));
    List<String> messages = messages();
    assertTrue(messages.contains("Using context birth as time reference."));
    assertTrue(messages.contains("Adding property to the context with key=\"birth\" and value=\""
        + BIRTH_TIME_FORMATTED + "\" to the LOCAL scope"));
  }

  @Test
  public void interpretationTimeIsUsedAsTimeReferenceByDefault() throws ActionException {
    long before = System.currentTimeMillis();
    action.begin(ic, "timestamp", timestampAttributes("now", PATTERN, null, "context"));
    long after = System.currentTimeMillis();

    // the value is the time of the call, formatted like the action does (US locale, default time zone)
    SimpleDateFormat format = new SimpleDateFormat(PATTERN, Locale.US);
    Set<String> possibleValues = new HashSet<String>();
    for (long t = Math.min(before, after); t <= Math.max(before, after); t++) {
      possibleValues.add(format.format(new Date(t)));
    }
    String value = context.getProperty("now");
    assertTrue(value + " not in " + possibleValues, possibleValues.contains(value));
    List<String> messages = messages();
    assertTrue(messages.contains("Using current interpretation time, i.e. now, as time reference."));
    assertFalse(messages.contains("Using context birth as time reference."));
  }

  @Test
  public void missingKeyIsReportedAndNoPropertyIsAdded() throws ActionException {
    action.begin(ic, "timestamp", timestampAttributes("", PATTERN, null, null));

    assertTrue(action.inError);
    assertOnlyErrors("Attribute named [key] cannot be empty");
    assertTrue(ic.getCopyOfPropertyMap().isEmpty());
  }

  @Test
  public void missingDatePatternIsReportedAndNoPropertyIsAdded() throws ActionException {
    action.begin(ic, "timestamp", timestampAttributes("stamp", null, null, null));

    assertTrue(action.inError);
    assertOnlyErrors("Attribute named [datePattern] cannot be empty");
    assertNull(ic.getProperty("stamp"));
  }

  private static DummyAttributes timestampAttributes(String key, String datePattern,
      String timeReference, String scope) {
    DummyAttributes atts = new DummyAttributes();
    atts.setValue(Action.KEY_ATTRIBUTE, key);
    atts.setValue(TimestampAction.DATE_PATTERN_ATTRIBUTE, datePattern);
    atts.setValue(TimestampAction.TIME_REFERENCE_ATTRIBUTE, timeReference);
    atts.setValue(Action.SCOPE_ATTRIBUTE, scope);
    return atts;
  }

  private List<String> messages() {
    List<String> messages = new ArrayList<String>();
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      messages.add(s.getMessage());
    }
    return messages;
  }

  private void assertOnlyErrors(String... expected) {
    List<String> errors = new ArrayList<String>();
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      if (s.getLevel() == Status.ERROR) {
        errors.add(s.getMessage());
      }
    }
    assertEquals(Arrays.asList(expected), errors);
    for (String message : messages()) {
      assertFalse(message, message.startsWith("Adding property"));
    }
  }
}
