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
package ch.qos.logback.core.sift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.status.Status;

public class SiftingJoranConfiguratorBaseTest {

  static final String NO_APPENDER_MSG =
      "No nested appenders found within the <sift> element in SiftingAppender.";
  static final String SEVERAL_APPENDERS_MSG =
      "Only and only one appender can be nested the <sift> element in SiftingAppender. See also "
          + CoreConstants.CODES_URL + "#1andOnly1";

  ContextBase context = new ContextBase();
  TestSiftingJoranConfigurator configurator = new TestSiftingJoranConfigurator();

  @Before
  public void setUp() {
    configurator.setContext(context);
  }

  @Test
  public void exactlyOneNestedAppenderIsAccepted() {
    configurator.oneAndOnlyOneCheck(Collections.singletonMap("A", "appender"));

    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void missingNestedAppenderIsReported() {
    configurator.oneAndOnlyOneCheck(Collections.emptyMap());

    assertErrorMessages(NO_APPENDER_MSG);
  }

  @Test
  public void severalNestedAppendersAreReported() {
    Map<String, String> appenderMap = new HashMap<String, String>();
    appenderMap.put("A", "appender A");
    appenderMap.put("B", "appender B");

    configurator.oneAndOnlyOneCheck(appenderMap);

    assertErrorMessages(SEVERAL_APPENDERS_MSG);
  }

  @Test
  public void reportsStopOnceTheErrorCountReachesMaxErrorCount() {
    Map<String, String> appenderMap = new HashMap<String, String>();
    appenderMap.put("A", "appender A");
    appenderMap.put("B", "appender B");

    configurator.oneAndOnlyOneCheck(Collections.emptyMap());
    configurator.oneAndOnlyOneCheck(appenderMap);
    // a valid map is not an error and does not count
    configurator.oneAndOnlyOneCheck(Collections.singletonMap("A", "appender"));
    configurator.oneAndOnlyOneCheck(Collections.emptyMap());
    // from here on errorEmmissionCount >= MAX_ERROR_COUNT (4)
    configurator.oneAndOnlyOneCheck(Collections.emptyMap());
    configurator.oneAndOnlyOneCheck(appenderMap);

    assertEquals(4, CoreConstants.MAX_ERROR_COUNT);
    assertErrorMessages(NO_APPENDER_MSG, SEVERAL_APPENDERS_MSG, NO_APPENDER_MSG);
  }

  @Test
  public void toStringShowsClassKeyAndValue() {
    assertEquals(TestSiftingJoranConfigurator.class.getName() + "{userid=alice}",
        configurator.toString());
  }

  private void assertErrorMessages(String... expectedMessages) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), expectedMessages.length, statuses.size());
    for (int i = 0; i < expectedMessages.length; i++) {
      assertEquals(Status.ERROR, statuses.get(i).getLevel());
      assertEquals(expectedMessages[i], statuses.get(i).getMessage());
    }
  }

  static class TestSiftingJoranConfigurator extends SiftingJoranConfiguratorBase<Object> {
    TestSiftingJoranConfigurator() {
      super("userid", "alice", Collections.<String, String>emptyMap());
    }

    @Override
    public Appender<Object> getAppender() {
      return null;
    }
  }
}
