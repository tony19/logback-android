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

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.helper.RenameUtil;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileOutputStream;

public class RenameUtilTest {

  Encoder<Object> encoder;
  Context context = new ContextBase();
  StatusChecker statusChecker = new StatusChecker(context);

  long currentTime = System.currentTimeMillis();
  int diff = RandomUtil.getPositiveInt();
  protected String randomOutputDirAsStr = CoreTestConstants.OUTPUT_DIR_PREFIX + diff
          + "/";
  protected File randomOutputDir = new File(randomOutputDirAsStr);

  @Before
  public void setUp() throws Exception {
    encoder = new EchoEncoder<Object>();
    // if this this the fist test run after 'build clean up' then the
    // OUTPUT_DIR_PREFIX might be not yet created
    randomOutputDir.mkdirs();
  }

  @Test
  public void renameToNonExistingDirectory() throws IOException, RolloverFailure {
    RenameUtil renameUtil = new RenameUtil();
    renameUtil.setContext(context);

    int diff2 = RandomUtil.getPositiveInt();
    File fromFile = File.createTempFile("from" + diff, "test",
            randomOutputDir);

    String randomTARGETDir = CoreTestConstants.OUTPUT_DIR_PREFIX + diff2;

    renameUtil.rename(fromFile.toString(), new File(randomTARGETDir + "/to.test").toString());
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    assertTrue(statusChecker.isErrorFree(0));
  }


  @Test
  @Ignore
  public void MANUAL_renamingOnDifferentVolumesOnLinux() throws IOException, RolloverFailure {
    RenameUtil renameUtil = new RenameUtil();
    renameUtil.setContext(context);

    String src = "/tmp/ramdisk/foo.txt";
    makeFile(src);

    renameUtil.rename(src, "/tmp/foo" + diff + ".txt");
    StatusPrinter.print(context);
  }

  @Test //  LOGBACK-1054
  public void renameLockedAbstractFile_LOGBACK_1054 () throws IOException, RolloverFailure {
    RenameUtil renameUtil = new RenameUtil();
    renameUtil.setContext(context);

    String abstractFileName = "abstract_pathname-" + diff;

    String src = CoreTestConstants.OUTPUT_DIR_PREFIX + abstractFileName;
    String target = src + ".target";

    makeFile(src);

    FileInputStream fisLock = new FileInputStream(src);
    renameUtil.rename(src, target);
    // release the lock
    fisLock.close();

    StatusPrinter.print(context);
    assertEquals(0, statusChecker.matchCount("Parent of target file ." + target + ". is null"));
  }

  @Test
  @Ignore
  public void MANUAL_renamingOnDifferentVolumesOnWindows() throws IOException, RolloverFailure {
    RenameUtil renameUtil = new RenameUtil();
    renameUtil.setContext(context);

    String src = "c:/tmp/foo.txt";
    makeFile(src);

    renameUtil.rename(src, "d:/tmp/foo" + diff + ".txt");
    StatusPrinter.print(context);
    assertTrue(statusChecker.isErrorFree(0));
  }

  private void makeFile(String src) throws IOException {
    FileOutputStream fos = new FileOutputStream(src);
    fos.write(("hello" + diff).getBytes());
    fos.close();
  }

}
