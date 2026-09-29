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
package ch.qos.logback.classic.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;

public class LoggerRemoteViewTest {

  @Test
  public void keepsTheNameAndTheRemoteViewOfTheContext() {
    LoggerContext lc = new LoggerContext();
    lc.setName("ctx");

    LoggerRemoteView view = new LoggerRemoteView("a.b.C", lc);

    assertEquals("a.b.C", view.getName());
    assertNotNull(view.getLoggerContextView());
    assertSame(lc.getLoggerContextRemoteView(), view.getLoggerContextView());
  }

  @Test
  public void contextWithoutRemoteViewFailsTheAssertionWhenAssertionsAreEnabled() {
    // Gradle runs unit tests with assertions enabled (-ea)
    assumeTrue(LoggerRemoteView.class.desiredAssertionStatus());

    AssertionError e = assertThrows(AssertionError.class,
        () -> new LoggerRemoteView("a.b.C", new ContextWithoutRemoteView()));
    assertNull(e.getMessage());
  }

  @Test
  public void contextWithoutRemoteViewYieldsNullViewWhenAssertionsAreDisabled() {
    assumeFalse(LoggerRemoteView.class.desiredAssertionStatus());

    LoggerRemoteView view = new LoggerRemoteView("a.b.C", new ContextWithoutRemoteView());

    assertEquals("a.b.C", view.getName());
    assertNull(view.getLoggerContextView());
  }

  static class ContextWithoutRemoteView extends LoggerContext {
    @Override
    public LoggerContextVO getLoggerContextRemoteView() {
      return null;
    }
  }
}
