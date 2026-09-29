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
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusUtil;

public class SiftingAppenderBaseTest {

  private final ContextBase context = new ContextBase();

  private final SiftingAppenderBase<Object> appender = new SiftingAppenderBase<Object>() {
    @Override
    protected long getTimestamp(Object event) {
      return 0;
    }

    @Override
    protected boolean eventMarksEndOfLife(Object event) {
      return false;
    }
  };

  @Test
  public void startWithoutDiscriminatorReportsAnErrorInsteadOfThrowing() {
    appender.setContext(context);

    appender.start();

    assertFalse(appender.isStarted());
    assertEquals(Status.ERROR, new StatusUtil(context).getHighestLevel(0));
  }
}
