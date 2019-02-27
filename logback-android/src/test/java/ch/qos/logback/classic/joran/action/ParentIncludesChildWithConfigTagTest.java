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
package ch.qos.logback.classic.joran.action;

import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Verifies that {@link FindIncludeAction} class can include
 * a child XML configuration that is enclosed within an {@code <configuration>}
 * tag.
 */
public class ParentIncludesChildWithConfigTagTest extends BaseIncludesTezt {

  public ParentIncludesChildWithConfigTagTest() {
    super(RESOURCE_DIR + "parent_by_config.xml");
  }

  @Test
  public void parentConfigIncludesChildLogcatAppender() throws JoranException {
    assertHasAppender("config-logcat", LogcatAppender.class);
  }

  @Test
  public void parentConfigIncludesChildFileAppender() throws JoranException {
    assertHasAppender("config-trace-log", FileAppender.class);
  }

  @Test
  public void contextHasChildLoggerConfig() {
    Logger logger = context.getLogger("org.example.test.Tester");
    assertThat(logger.isInfoEnabled(), is(true));
  }
}