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
package ch.qos.logback.core.recovery;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;

public class ResilientOutputStreamBaseTest {

  /** A time far past any back-off period started by the recovery coordinator. */
  static final long FAR_FUTURE = Long.MAX_VALUE / 2;
  static final String DESCRIPTION = "scripted stream";

  Context context = new ContextBase();
  FaultyOutputStream target = new FaultyOutputStream();

  /** An output stream whose writes and close can be made to fail. */
  static class FaultyOutputStream extends OutputStream {
    final ByteArrayOutputStream written = new ByteArrayOutputStream();
    IOException writeFailure;
    IOException closeFailure;
    boolean closed;

    @Override
    public void write(int b) throws IOException {
      if (writeFailure != null) {
        throw writeFailure;
      }
      written.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (writeFailure != null) {
        throw writeFailure;
      }
      written.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
      closed = true;
      if (closeFailure != null) {
        throw closeFailure;
      }
    }
  }

  /** A resilient stream whose replacement stream (or failure to open one) is set by the test. */
  static class ScriptedResilientOutputStream extends ResilientOutputStreamBase {
    OutputStream nextStream;
    IOException openFailure;
    int openCount;

    ScriptedResilientOutputStream(OutputStream initial) {
      this.os = initial;
    }

    @Override
    String getDescription() {
      return DESCRIPTION;
    }

    @Override
    OutputStream openNewOutputStream() throws IOException {
      openCount++;
      if (openFailure != null) {
        throw openFailure;
      }
      return nextStream;
    }
  }

  private ScriptedResilientOutputStream newStream(OutputStream initial) {
    ScriptedResilientOutputStream stream = new ScriptedResilientOutputStream(initial);
    stream.setContext(context);
    return stream;
  }

  private static RecoveryCoordinator recoveryCoordinatorOf(ResilientOutputStreamBase stream) throws Exception {
    Field field = ResilientOutputStreamBase.class.getDeclaredField("recoveryCoordinator");
    field.setAccessible(true);
    return (RecoveryCoordinator) field.get(stream);
  }

  private Status lastStatus() {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    return statuses.get(statuses.size() - 1);
  }

  private void assertLastStatus(int level, String message) {
    Status status = lastStatus();
    assertEquals(level, status.getLevel());
    assertEquals(message, status.getMessage());
  }

  /** Makes the stream fail once so that it is presumed in error. */
  private void failOnce(ScriptedResilientOutputStream stream) {
    target.writeFailure = new IOException("disk full");
    stream.write('x');
    target.writeFailure = null;
  }

  @Test
  public void singleByteIsWrittenToUnderlyingStream() {
    ScriptedResilientOutputStream stream = newStream(target);

    stream.write('a');

    assertArrayEquals(new byte[] {'a'}, target.written.toByteArray());
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void failedSingleByteWriteIsReportedAsError() {
    ScriptedResilientOutputStream stream = newStream(target);
    IOException failure = new IOException("disk full");
    target.writeFailure = failure;

    stream.write('a');

    assertLastStatus(Status.ERROR, "IO failure while writing to " + DESCRIPTION);
    assertSame(failure, lastStatus().getThrowable());
  }

  @Test
  public void failedByteArrayWriteIsReportedAsError() {
    ScriptedResilientOutputStream stream = newStream(target);
    IOException failure = new IOException("disk full");
    target.writeFailure = failure;

    stream.write(new byte[] {'a', 'b', 'c'}, 0, 3);

    assertLastStatus(Status.ERROR, "IO failure while writing to " + DESCRIPTION);
    assertSame(failure, lastStatus().getThrowable());
  }

  @Test
  public void singleByteWritesAreDroppedWhileBackingOff() throws Exception {
    ScriptedResilientOutputStream stream = newStream(target);
    failOnce(stream);
    recoveryCoordinatorOf(stream).setCurrentTime(0);

    stream.write('b');

    assertEquals(0, target.written.size());
    assertEquals(0, stream.openCount);
  }

  @Test
  public void singleByteWriteRecoversOnceBackOffHasElapsed() throws Exception {
    ScriptedResilientOutputStream stream = newStream(target);
    FaultyOutputStream replacement = new FaultyOutputStream();
    stream.nextStream = replacement;
    failOnce(stream);
    recoveryCoordinatorOf(stream).setCurrentTime(FAR_FUTURE);

    // this write triggers the recovery but is itself dropped
    stream.write('b');

    assertTrue(target.closed);
    assertEquals(1, stream.openCount);
    assertEquals(0, replacement.written.size());
    assertLastStatus(Status.INFO, "Attempting to recover from IO failure on " + DESCRIPTION);

    // the next write goes to the reopened stream and ends the failed state
    stream.write('c');

    assertArrayEquals(new byte[] {'c'}, replacement.written.toByteArray());
    assertLastStatus(Status.INFO, "Recovered from IO failure on " + DESCRIPTION);
  }

  @Test
  public void recoveryProceedsWhenClosingTheBrokenStreamFails() throws Exception {
    ScriptedResilientOutputStream stream = newStream(target);
    FaultyOutputStream replacement = new FaultyOutputStream();
    stream.nextStream = replacement;
    target.closeFailure = new IOException("cannot close");
    failOnce(stream);
    recoveryCoordinatorOf(stream).setCurrentTime(FAR_FUTURE);

    stream.write('b');
    stream.write('c');

    assertTrue(target.closed);
    assertEquals(1, stream.openCount);
    assertArrayEquals(new byte[] {'c'}, replacement.written.toByteArray());
  }

  @Test
  public void failureToReopenIsReportedAndStreamStaysInError() throws Exception {
    ScriptedResilientOutputStream stream = newStream(target);
    IOException openFailure = new IOException("cannot reopen");
    stream.openFailure = openFailure;
    failOnce(stream);
    recoveryCoordinatorOf(stream).setCurrentTime(FAR_FUTURE);

    stream.write('b');

    assertLastStatus(Status.ERROR, "Failed to open " + DESCRIPTION);
    assertSame(openFailure, lastStatus().getThrowable());

    // still presumed in error: the write is not passed on to the old stream
    stream.write('c');

    assertEquals(0, target.written.size());
  }

  @Test
  public void flushAndCloseWithoutUnderlyingStreamDoNothing() throws IOException {
    ScriptedResilientOutputStream stream = newStream(null);

    stream.flush();
    stream.close();

    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void statusesWithoutContextAreDroppedWithASingleConsoleWarning() throws Exception {
    ScriptedResilientOutputStream stream = new ScriptedResilientOutputStream(target);
    ByteArrayOutputStream console = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    String warning = "LOGBACK: No context given for " + stream;
    System.setOut(new PrintStream(console, true, "UTF-8"));
    try {
      // the first status is dropped with a warning...
      stream.addStatus(new InfoStatus("first", this));
      String afterFirst = console.toString("UTF-8");
      assertTrue(afterFirst, afterFirst.contains(warning));

      // ...and later ones are dropped silently
      stream.addStatus(new InfoStatus("second", this));
      assertEquals(afterFirst, console.toString("UTF-8"));
    } finally {
      System.setOut(originalOut);
    }
  }

  @Test
  public void statusesAreDroppedWhenContextHasNoStatusManager() {
    Context contextWithoutStatusManager = mock(Context.class);
    ScriptedResilientOutputStream stream = new ScriptedResilientOutputStream(target);
    stream.setContext(contextWithoutStatusManager);

    stream.addStatus(new InfoStatus("dropped", this));

    verify(contextWithoutStatusManager).getStatusManager();
  }

  @Test
  public void contextIsTheOneSet() {
    ScriptedResilientOutputStream stream = newStream(target);

    assertSame(context, stream.getContext());
  }
}
