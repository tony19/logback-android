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
package ch.qos.logback.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Tests the {@code syncOnFlush} option of {@link FileAppender}, which fsyncs
 * the log file on every flush so already-written events survive an abrupt
 * power-off (issue #371).
 */
public class FileAppenderSyncOnFlushTest {

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private final Context context = new ContextBase();

  @Test
  public void eventsAreWrittenAndDurableWithSyncOnFlush() throws IOException {
    File logFile = new File(tmpDir.getRoot(), "sync.log");

    FileAppender<Object> appender = new FileAppender<Object>();
    appender.setContext(context);
    appender.setName("SYNC");
    appender.setFile(logFile.getPath());
    appender.setSyncOnFlush(true);
    EchoEncoder<Object> encoder = new EchoEncoder<Object>();
    encoder.setContext(context);
    encoder.start();
    appender.setEncoder(encoder);
    appender.start();

    assertTrue(appender.isStarted());
    appender.doAppend("event 1");
    appender.doAppend("event 2");

    // with immediateFlush (default) + syncOnFlush, both events must be on
    // disk before stop() — read the file while the appender is still open
    String written = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
    assertEquals("event 1" + CoreConstants.LINE_SEPARATOR
        + "event 2" + CoreConstants.LINE_SEPARATOR, written);

    appender.stop();
    StatusChecker checker = new StatusChecker(context);
    assertTrue(checker.isErrorFree(0));
  }
}
