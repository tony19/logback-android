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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ch.qos.logback.core.testUtil.EnvUtilForTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.ResilienceUtil;
import ch.qos.logback.core.util.StatusPrinter;

public class FileAppenderResilience_AS_ROOT_Test {

  static String MOUNT_POINT = "/mnt/loop/";

  static String LONG_STR = " xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

  static String PATH_LOOPFS_SCRIPT = "/home/ceki/java/logback/logback-core/src/test/loopfs.sh";

  enum LoopFSCommand {
    setup, shake, teardown;
  }

  Context context = new ContextBase();
  int diff = RandomUtil.getPositiveInt();
  String outputDirStr = MOUNT_POINT + "resilience-" + diff + "/";
  String logfileStr = outputDirStr + "output.log";

  FileAppender<Object> fa = new FileAppender<Object>();

  static boolean isConformingHost() {
    return EnvUtilForTests.isLocalHostNameInList(new String[]{"haro"});
  }

  @Before
  public void setUp() throws IOException, InterruptedException {
    if (!isConformingHost()) {
      return;
    }
    Process p = runLoopFSScript(LoopFSCommand.setup);
    p.waitFor();

    dump("/tmp/loopfs.log");

    fa.setContext(context);
    File outputDir = new File(outputDirStr);
    outputDir.mkdirs();
    System.out.println("FileAppenderResilienceTest output dir [" + outputDirStr
        + "]");

    fa.setName("FILE");
    fa.setEncoder(new EchoEncoder<Object>());
    fa.setFile(logfileStr);
    fa.start();
  }

  void dump(String file) throws IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      int r;
      while ((r = fis.read()) != -1) {
        char c = (char) r;
        System.out.print(c);
      }
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
  }

  @After
  public void tearDown() throws IOException, InterruptedException {
    if (!isConformingHost()) {
      return;
    }
    StatusPrinter.print(context);
    fa.stop();
    Process p = runLoopFSScript(LoopFSCommand.teardown);
    p.waitFor();
    System.out.println("Tearing down");
  }

  static int TOTAL_DURATION = 5000;
  static int NUM_STEPS = 500;
  static int DELAY = TOTAL_DURATION / NUM_STEPS;

  @Test
  public void go() throws IOException, InterruptedException {
    if (!isConformingHost()) {
      return;
    }
    Process p = runLoopFSScript(LoopFSCommand.shake);
    for (int i = 0; i < NUM_STEPS; i++) {
      fa.append(String.valueOf(i) + LONG_STR);
      Thread.sleep(DELAY);
    }
    p.waitFor();
    // the extrernal script has the file system ready for IO 50% of the time
    double bestCase = 0.5;
    ResilienceUtil.verify(logfileStr, "^(\\d{1,3}) x*$", NUM_STEPS, bestCase*0.6);
    System.out.println("Done go");
  }

  // the loopfs script is tightly coupled with the host machine
  // it needs to be Unix, with sudo privileges granted to the script
  Process runLoopFSScript(LoopFSCommand cmd) throws IOException,
      InterruptedException {
    // causing a NullPointerException is better than locking the whole
    // machine which the next operation can and will do.
    if (!isConformingHost()) {
      return null;
    }
    ProcessBuilder pb = new ProcessBuilder();
    pb.command("/usr/bin/sudo", PATH_LOOPFS_SCRIPT, cmd.toString());
    return pb.start();
  }
}
