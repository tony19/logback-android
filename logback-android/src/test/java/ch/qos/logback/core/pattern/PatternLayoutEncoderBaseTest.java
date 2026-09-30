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
package ch.qos.logback.core.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;

public class PatternLayoutEncoderBaseTest {

  private final Context context = new ContextBase();
  private final PatternLayoutEncoderBase<Object> encoder = new PatternLayoutEncoderBase<Object>();

  @Before
  public void setUp() {
    encoder.setContext(context);
  }

  @Test
  public void patternIsNullUntilSet() {
    assertNull(encoder.getPattern());
    encoder.setPattern("%msg%n");
    assertEquals("%msg%n", encoder.getPattern());
  }

  @Test
  public void outputPatternAsHeaderIsOffByDefault() {
    assertFalse(encoder.isOutputPatternAsHeader());
    assertFalse(encoder.isOutputPatternAsPresentationHeader());
  }

  @Test
  public void setOutputPatternAsHeaderTogglesBothGettersWithoutWarning() {
    encoder.setOutputPatternAsHeader(true);
    assertTrue(encoder.isOutputPatternAsHeader());
    assertTrue(encoder.isOutputPatternAsPresentationHeader());

    encoder.setOutputPatternAsHeader(false);
    assertFalse(encoder.isOutputPatternAsHeader());
    assertFalse(encoder.isOutputPatternAsPresentationHeader());

    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void deprecatedSetterSetsTheFlagAndWarns() {
    encoder.setOutputPatternAsPresentationHeader(true);

    assertTrue(encoder.isOutputPatternAsHeader());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.WARN, statuses.get(0).getLevel());
    assertEquals("[outputPatternAsPresentationHeader] property is deprecated. Please use [outputPatternAsHeader] option instead.",
        statuses.get(0).getMessage());
  }

  @Test
  public void layoutCannotBeSet() {
    PatternLayoutBase<Object> layout = new PatternLayoutBase<Object>() {
      @Override
      public Map<String, String> getDefaultConverterMap() {
        return new HashMap<String, String>();
      }

      @Override
      public String doLayout(Object event) {
        return "";
      }
    };

    UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
        () -> encoder.setLayout(layout));
    assertEquals("one cannot set the layout of " + PatternLayoutEncoderBase.class.getName(), e.getMessage());
    assertNull(encoder.getLayout());
  }
}
