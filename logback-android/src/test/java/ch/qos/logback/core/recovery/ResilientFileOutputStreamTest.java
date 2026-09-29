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

public class ResilientFileOutputStreamTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  Context context = new ContextBase();
  File file;
  ResilientFileOutputStream stream;

  @Before
  public void setUp() throws IOException {
    file = tmp.newFile("resilient.log");
    stream = new ResilientFileOutputStream(file, true, 8192);
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
}
