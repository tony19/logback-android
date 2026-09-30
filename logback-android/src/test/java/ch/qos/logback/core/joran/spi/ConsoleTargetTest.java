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
package ch.qos.logback.core.joran.spi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class ConsoleTargetTest {

  /** A console stand-in that counts how often it is flushed. */
  private static class FlushCountingPrintStream extends PrintStream {
    int flushes;

    FlushCountingPrintStream(OutputStream out) {
      super(out);
    }

    @Override
    public void flush() {
      flushes++;
      super.flush();
    }
  }

  private PrintStream originalOut;
  private PrintStream originalErr;
  private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
  private final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
  private final FlushCountingPrintStream out = new FlushCountingPrintStream(capturedOut);
  private final FlushCountingPrintStream err = new FlushCountingPrintStream(capturedErr);

  @Before
  public void redirectConsole() {
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(out);
    System.setErr(err);
  }

  @After
  public void restoreConsole() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  private static void writeAllWays(OutputStream stream) throws IOException {
    stream.write('A');
    stream.write(new byte[] {'B', 'C'});
    stream.write(new byte[] {'x', 'D', 'E', 'x'}, 1, 2);
    stream.flush();
  }

  private static final byte[] EXPECTED = {'A', 'B', 'C', 'D', 'E'};

  @Test
  public void systemOutStreamWritesToTheCurrentSystemOut() throws IOException {
    writeAllWays(ConsoleTarget.SystemOut.getStream());

    assertArrayEquals(EXPECTED, capturedOut.toByteArray());
    assertEquals(0, capturedErr.size());
  }

  @Test
  public void systemErrStreamWritesToTheCurrentSystemErr() throws IOException {
    writeAllWays(ConsoleTarget.SystemErr.getStream());

    assertArrayEquals(EXPECTED, capturedErr.toByteArray());
    assertEquals(0, capturedOut.size());
  }

  @Test
  public void systemOutStreamFlushesTheCurrentSystemOut() throws IOException {
    int before = out.flushes;
    ConsoleTarget.SystemOut.getStream().flush();

    assertTrue(out.flushes > before);
  }

  @Test
  public void systemErrStreamFlushesTheCurrentSystemErr() throws IOException {
    int before = err.flushes;
    ConsoleTarget.SystemErr.getStream().flush();

    assertTrue(err.flushes > before);
  }

  @Test
  public void findByNameIgnoresCase() {
    assertSame(ConsoleTarget.SystemOut, ConsoleTarget.findByName("System.out"));
    assertSame(ConsoleTarget.SystemOut, ConsoleTarget.findByName("SYSTEM.OUT"));
    assertSame(ConsoleTarget.SystemErr, ConsoleTarget.findByName("system.err"));
  }

  @Test
  public void findByNameReturnsNullForUnknownNames() {
    assertNull(ConsoleTarget.findByName("System.in"));
    assertNull(ConsoleTarget.findByName("SystemOut"));
  }

  @Test
  public void nameAndToStringAreTheDisplayName() {
    assertEquals("System.out", ConsoleTarget.SystemOut.getName());
    assertEquals("System.err", ConsoleTarget.SystemErr.getName());
    assertEquals("System.out", ConsoleTarget.SystemOut.toString());
    assertEquals("System.err", ConsoleTarget.SystemErr.toString());
  }
}
