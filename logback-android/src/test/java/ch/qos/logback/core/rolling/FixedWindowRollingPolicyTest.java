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
package ch.qos.logback.core.rolling;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class FixedWindowRollingPolicyTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  Context context = new ContextBase();
  StatusChecker checker = new StatusChecker(context);
  RollingFileAppender<Object> rfa = new RollingFileAppender<Object>();
  FixedWindowRollingPolicy fwrp = new FixedWindowRollingPolicy();
  String activeFileName;

  @Before
  public void setUp() {
    activeFileName = path("active.log");
    rfa.setContext(context);
    fwrp.setContext(context);
    fwrp.setParent(rfa);
  }

  private String path(String name) {
    return new File(tmp.getRoot(), name).getAbsolutePath();
  }

  private static void write(String fileName, String content) throws IOException {
    Files.write(new File(fileName).toPath(), content.getBytes("UTF-8"));
  }

  private static String read(String fileName) throws IOException {
    return new String(Files.readAllBytes(new File(fileName).toPath()), "UTF-8");
  }

  private void startWithWindow(String fileNamePattern, int minIndex, int maxIndex) {
    rfa.setFile(activeFileName);
    fwrp.setFileNamePattern(fileNamePattern);
    fwrp.setMinIndex(minIndex);
    fwrp.setMaxIndex(maxIndex);
    fwrp.start();
  }

  private static byte[] readFully(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int n;
    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }

  @Test
  public void defaultWindowIsOneToSeven() {
    assertEquals(1, fwrp.getMinIndex());
    assertEquals(7, fwrp.getMaxIndex());
  }

  @Test
  public void startWithoutFileNamePatternIsRejected() {
    rfa.setFile(activeFileName);

    IllegalStateException e = assertThrows(IllegalStateException.class, fwrp::start);

    assertEquals(FixedWindowRollingPolicy.FNP_NOT_SET + CoreConstants.SEE_FNP_NOT_SET, e.getMessage());
    checker.assertContainsMatch(Status.ERROR, Pattern.quote(FixedWindowRollingPolicy.FNP_NOT_SET));
    checker.assertContainsMatch(Status.ERROR, Pattern.quote(CoreConstants.SEE_FNP_NOT_SET));
    assertFalse(fwrp.isStarted());
  }

  @Test
  public void startInPrudentModeIsRejected() {
    rfa.setPrudent(true);
    rfa.setFile(activeFileName);
    fwrp.setFileNamePattern(path("archive.%i.log"));

    IllegalStateException e = assertThrows(IllegalStateException.class, fwrp::start);

    assertEquals("Prudent mode is not supported.", e.getMessage());
    checker.assertContainsMatch(Status.ERROR, "Prudent mode is not supported with FixedWindowRollingPolicy.");
    checker.assertContainsMatch(Status.ERROR, Pattern.quote(FixedWindowRollingPolicy.PRUDENT_MODE_UNSUPPORTED));
    assertFalse(fwrp.isStarted());
  }

  @Test
  public void maxIndexBelowMinIndexIsRaisedToMinIndex() {
    startWithWindow(path("archive.%i.log"), 3, 2);

    assertTrue(fwrp.isStarted());
    assertEquals(3, fwrp.getMinIndex());
    assertEquals(3, fwrp.getMaxIndex());
    checker.assertContainsMatch(Status.WARN, "MaxIndex \\(2\\) cannot be smaller than MinIndex \\(3\\).");
    checker.assertContainsMatch(Status.WARN, "Setting maxIndex to equal minIndex.");
  }

  @Test
  public void oversizedWindowIsReducedToMaxWindowSize() {
    startWithWindow(path("archive.%i.log"), 2, 30);

    assertTrue(fwrp.isStarted());
    assertEquals(2, fwrp.getMinIndex());
    assertEquals(22, fwrp.getMaxIndex());
    checker.assertContainsMatch(Status.WARN, "Large window sizes are not allowed.");
    checker.assertContainsMatch(Status.WARN, "MaxIndex reduced to 22");
  }

  @Test
  public void fileNamePatternWithoutIntegerTokenIsRejected() {
    rfa.setFile(activeFileName);
    fwrp.setFileNamePattern(path("archive.log"));

    IllegalStateException e = assertThrows(IllegalStateException.class, fwrp::start);

    assertEquals("FileNamePattern [" + path("archive.log") + "] does not contain a valid IntegerToken",
        e.getMessage());
    assertFalse(fwrp.isStarted());
  }

  @Test
  public void rolloverShiftsArchivesAndDropsTheOneAtMaxIndex() throws IOException {
    startWithWindow(path("archive.%i.log"), 1, 3);
    write(activeFileName, "active");
    write(path("archive.1.log"), "one");
    // archive.2.log is missing, so nothing is renamed onto archive.3.log
    write(path("archive.3.log"), "three");

    fwrp.rollover();

    assertFalse(new File(activeFileName).exists());
    assertEquals("active", read(path("archive.1.log")));
    assertEquals("one", read(path("archive.2.log")));
    assertFalse(new File(path("archive.3.log")).exists());
    checker.assertContainsMatch(Status.INFO,
        "Skipping roll-over for inexistent file " + Pattern.quote(path("archive.2.log")));
  }

  @Test
  public void rolloverWithNegativeMaxIndexLeavesFilesAlone() throws IOException {
    startWithWindow(path("archive.%i.log"), 1, 3);
    fwrp.setMaxIndex(-1);
    write(activeFileName, "active");

    fwrp.rollover();

    assertEquals("active", read(activeFileName));
    assertEquals(1, tmp.getRoot().list().length);
  }

  @Test
  public void rolloverWithoutCompressionRenamesActiveFile() throws IOException {
    startWithWindow(path("archive.%i.log"), 1, 3);
    write(activeFileName, "active");

    fwrp.rollover();

    assertFalse(new File(activeFileName).exists());
    assertEquals("active", read(path("archive.1.log")));
  }

  @Test
  public void rolloverWithGzCompressionCompressesActiveFile() throws IOException {
    startWithWindow(path("archive.%i.log.gz"), 1, 3);
    write(activeFileName, "active");

    fwrp.rollover();

    assertFalse(new File(activeFileName).exists());
    try (InputStream in = new GZIPInputStream(new FileInputStream(path("archive.1.log.gz")))) {
      assertArrayEquals("active".getBytes("UTF-8"), readFully(in));
    }
  }

  @Test
  public void rolloverWithZipCompressionCompressesActiveFile() throws IOException {
    startWithWindow(path("archive.%i.log.zip"), 1, 3);
    write(activeFileName, "active");

    fwrp.rollover();

    assertFalse(new File(activeFileName).exists());
    try (ZipInputStream in = new ZipInputStream(new FileInputStream(path("archive.1.log.zip")))) {
      ZipEntry entry = in.getNextEntry();
      assertTrue(entry.getName(), entry.getName().matches("archive\\.\\d{4}-\\d{2}-\\d{2}_\\d{4}\\.log"));
      assertArrayEquals("active".getBytes("UTF-8"), readFully(in));
    }
  }
}
