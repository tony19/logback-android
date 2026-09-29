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
package ch.qos.logback.core.pattern.color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ANSIConstantsTest {

  @Test
  public void isInstantiable() {
    // the implicit public constructor is part of the published API
    assertNotNull(new ANSIConstants());
  }

  @Test
  public void escapeSequenceDelimiters() {
    assertEquals("\u001b[", ANSIConstants.ESC_START);
    assertEquals("m", ANSIConstants.ESC_END);
    assertEquals("1;", ANSIConstants.BOLD);
  }

  @Test
  public void foregroundColorCodesFollowTheAnsiStandard() {
    assertEquals("30", ANSIConstants.BLACK_FG);
    assertEquals("31", ANSIConstants.RED_FG);
    assertEquals("32", ANSIConstants.GREEN_FG);
    assertEquals("33", ANSIConstants.YELLOW_FG);
    assertEquals("34", ANSIConstants.BLUE_FG);
    assertEquals("35", ANSIConstants.MAGENTA_FG);
    assertEquals("36", ANSIConstants.CYAN_FG);
    assertEquals("37", ANSIConstants.WHITE_FG);
    assertEquals("39", ANSIConstants.DEFAULT_FG);
  }
}
