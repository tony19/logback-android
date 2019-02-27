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
package ch.qos.logback.core.status;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class StatusUtilTest {

  Context context = new ContextBase();
  StatusUtil statusUtil = new StatusUtil(context);

  @Test
  public void emptyStatusListShouldResultInNotFound() {
    assertEquals(-1, statusUtil.timeOfLastReset());
  }

  @Test
  public void withoutResetsStatusUtilShouldReturnNotFound() {
    context.getStatusManager().add(new InfoStatus("test", this));
    assertEquals(-1, statusUtil.timeOfLastReset());
  }

  @Test
  public void statusListShouldReturnLastResetTime() {
    context.getStatusManager().add(new InfoStatus("test", this));
    long resetTime = System.currentTimeMillis();
    context.getStatusManager().add(new InfoStatus(CoreConstants.RESET_MSG_PREFIX, this));
    context.getStatusManager().add(new InfoStatus("bla", this));
    assertTrue(resetTime <= statusUtil.timeOfLastReset());
  }

}
