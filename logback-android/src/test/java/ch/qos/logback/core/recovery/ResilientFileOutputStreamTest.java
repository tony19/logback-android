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

import static ch.qos.logback.core.recovery.ResilientOutputStreamBaseTest.FAR_FUTURE;
import static ch.qos.logback.core.recovery.ResilientOutputStreamBaseTest.recoveryCoordinatorOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.SyncFailedException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class ResilientFileOutputStreamTest {

  static final int BUFFER_SIZE = 8192;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  Context context = new ContextBase();
  File file;
  ResilientFileOutputStream stream;

  @Before
  public void setUp() throws IOException {
    file = tmp.newFile("resilient.log");
    stream = new ResilientFileOutputStream(file, true, BUFFER_SIZE);
    stream.setContext(context);
  }

  @After
  public void tearDown() throws IOException {
    if (stream.os != null) {
      stream.os.close();
    }
  }

  @Test
  public void fileIsTheOneBeingWritten() {
    assertSame(file, stream.getFile());
  }

  @Test
  public void toStringIdentifiesTheInstance() {
    assertEquals("c.q.l.c.recovery.ResilientFileOutputStream@" + System.identityHashCode(stream),
        stream.toString());
  }

  @Test
  public void channelIsNullWithoutUnderlyingStream() throws IOException {
    FileChannel channel = stream.getChannel();
    assertNotNull(channel);

    stream.os = null;

    assertNull(stream.getChannel());
    channel.close();
  }

  @Test
  public void failedSyncOnFlushIsReportedAsError() throws IOException {
    stream.setSyncOnFlush(true);
    // closing the channel also invalidates the file descriptor that flush() syncs
    stream.getChannel().close();

    stream.flush();

    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    Status status = statuses.get(0);
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals("IO failure while writing to file [" + file + "]", status.getMessage());
    assertTrue(String.valueOf(status.getThrowable()), status.getThrowable() instanceof SyncFailedException);
  }

  @Test
  public void syncOnFlushIsSkippedWithoutFileStream() throws Exception {
    stream.setSyncOnFlush(true);
    stream.write(new byte[] {'o', 'k'});
    Field fos = ResilientFileOutputStream.class.getDeclaredField("fos");
    fos.setAccessible(true);
    fos.set(stream, null);

    stream.flush();

    assertEquals(0, context.getStatusManager().getCount());
    assertArrayEquals(new byte[] {'o', 'k'}, Files.readAllBytes(file.toPath()));
  }

  /**
   * Makes the stream fail on a write that leaves nothing buffered, as a
   * prudent FileAppender's failure to lock a closed channel does: a write as
   * large as the buffer goes straight to the (closed) file stream.
   */
  private static void failWithNothingBuffered(ResilientFileOutputStream stream) throws IOException {
    stream.getChannel().close();
    stream.write(new byte[BUFFER_SIZE], 0, BUFFER_SIZE);
  }

  @Test
  public void failedRecoveryLeavesStreamInErrorAndBackingOff() throws Exception {
    File dir = tmp.newFolder("gone");
    File goneFile = new File(dir, "gone.log");
    ResilientFileOutputStream goneStream = new ResilientFileOutputStream(goneFile, true, BUFFER_SIZE);
    goneStream.setContext(context);
    failWithNothingBuffered(goneStream);
    // the file cannot be reopened
    assertTrue(goneFile.delete());
    assertTrue(dir.delete());
    RecoveryCoordinator coordinator = recoveryCoordinatorOf(goneStream);
    coordinator.setCurrentTime(FAR_FUTURE);

    // tries to reopen the file, and fails
    goneStream.write('a');
    // still presumed in error, and backing off from that attempt
    goneStream.write('b');

    StatusChecker checker = new StatusChecker(context);
    assertEquals(1, checker.matchCount("Attempting to recover from IO failure"));
    checker.assertContainsMatch(Status.ERROR, "Failed to open file");
    // closing the broken stream to reopen it must not count as recovering
    // from the failure, which would restart the back-off and the status count
    checker.assertNoMatch("Recovered from IO failure");
    assertSame(coordinator, recoveryCoordinatorOf(goneStream));
  }

  @Test
  public void recoveryIsReportedOnceWriteToReopenedFileSucceeds() throws Exception {
    failWithNothingBuffered(stream);
    recoveryCoordinatorOf(stream).setCurrentTime(FAR_FUTURE);

    // reopens the file, and is itself dropped
    stream.write('a');

    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.INFO, "Attempting to recover from IO failure");
    checker.assertNoMatch("Recovered from IO failure");

    stream.write('b');
    stream.flush();

    checker.assertContainsMatch(Status.INFO, "Recovered from IO failure");
    assertArrayEquals(new byte[] {'b'}, Files.readAllBytes(file.toPath()));
  }
}
