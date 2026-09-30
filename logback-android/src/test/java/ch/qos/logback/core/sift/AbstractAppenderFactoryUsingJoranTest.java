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
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ch.qos.logback.core.joran.event.SaxEvent;

public class AbstractAppenderFactoryUsingJoranTest {

  SaxEvent siftStart = mock(SaxEvent.class);
  SaxEvent appenderStart = mock(SaxEvent.class);
  SaxEvent appenderEnd = mock(SaxEvent.class);
  SaxEvent siftEnd = mock(SaxEvent.class);

  @Test
  public void eventListExcludesTheEnclosingSiftElement() {
    Map<String, String> parentProperties = Collections.singletonMap("p", "v");
    TestAppenderFactory factory = new TestAppenderFactory(
        Arrays.asList(siftStart, appenderStart, appenderEnd, siftEnd), "userid", parentProperties);

    assertEquals(Arrays.asList(appenderStart, appenderEnd), factory.getEventList());
    assertEquals("userid", factory.key);
    assertSame(parentProperties, factory.parentPropertyMap);
  }

  @Test
  public void eventListOfAnEmptySiftElementIsEmpty() {
    TestAppenderFactory factory = new TestAppenderFactory(
        Arrays.asList(siftStart, siftEnd), "userid", Collections.<String, String>emptyMap());

    assertEquals(Collections.emptyList(), factory.getEventList());
  }

  static class TestAppenderFactory extends AbstractAppenderFactoryUsingJoran<Object> {
    TestAppenderFactory(List<SaxEvent> eventList, String key, Map<String, String> parentPropertyMap) {
      super(eventList, key, parentPropertyMap);
    }

    @Override
    public SiftingJoranConfiguratorBase<Object> getSiftingJoranConfigurator(String k) {
      throw new UnsupportedOperationException();
    }
  }
}
