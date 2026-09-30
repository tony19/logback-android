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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.CoreConstants;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OnErrorConsoleStatusListenerTest {

  private final PrintStream originalErr = System.err;
  private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
  private PrintStream capturingErr;

  private final OnErrorConsoleStatusListener listener = new OnErrorConsoleStatusListener();

  @Before
  public void setUp() {
    capturingErr = OnPrintStreamStatusListenerBaseTest.newUtf8PrintStream(captured);
    System.setErr(capturingErr);
  }

  @After
  public void tearDown() {
    System.setErr(originalErr);
    capturingErr.close();
  }

  @Test
  public void printStreamIsTheCurrentSystemErr() {
    assertSame(capturingErr, listener.getPrintStream());
  }

  @Test
  public void startedListenerPrintsStatusesToSystemErr() throws Exception {
    listener.start();

    listener.addStatusEvent(new WarnStatus("hello", "origin"));

    String output = captured.toString("UTF-8");
    assertTrue(output, output.endsWith("|-WARN in origin - hello" + CoreConstants.LINE_SEPARATOR));
  }
}
