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
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.core.contention.RunnableWithCounterAndDone;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.recovery.RecoveryCoordinator;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.ResilienceUtil;

public class FileAppenderResilienceTest {

  FileAppender<Object> fa = new FileAppender<Object>();
  Context context = new ContextBase();
  int diff = RandomUtil.getPositiveInt();
  String outputDirStr = CoreTestConstants.OUTPUT_DIR_PREFIX + "resilience-"
      + diff + "/";

  // String outputDirStr = "\\\\192.168.1.3\\lbtest\\" + "resilience-"+ diff +
  // "/";;
  String logfileStr = outputDirStr + "output.log";

  @Before
  public void setUp() throws InterruptedException {
    File outputDir = new File(outputDirStr);
    outputDir.mkdirs();

    fa.setContext(context);
    fa.setName("FILE");
    fa.setEncoder(new EchoEncoder<Object>());
    fa.setFile(logfileStr);
    fa.start();
  }

  @Test
  @Ignore
  public void manual() throws InterruptedException, IOException {
    Runner runner = new Runner(fa);
    Thread t = new Thread(runner);
    t.start();

    while (true) {
      Thread.sleep(110);
    }
  }

  @Test
  public void smoke() throws InterruptedException, IOException {
    Runner runner = new Runner(fa);
    Thread t = new Thread(runner);
    t.start();

    double delayCoefficient = 2.0;
    for (int i = 0; i < 5; i++) {
      Thread.sleep((int)(RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN * delayCoefficient));
      closeLogFileOnPurpose();
    }
    runner.setDone(true);
    t.join();

    double bestCaseSuccessRatio = 1/delayCoefficient;
    // expect to loose at most 35% of the events
    double lossinessFactor = 0.35;
    double resilianceFactor = (1-lossinessFactor);

    ResilienceUtil
              .verify(logfileStr, "^hello (\\d{1,5})$", runner.getCounter(), bestCaseSuccessRatio * resilianceFactor);
  }

  private void closeLogFileOnPurpose() throws IOException {
    ResilientFileOutputStream resilientFOS = (ResilientFileOutputStream) fa
      .getOutputStream();
    FileChannel fileChannel = resilientFOS.getChannel();
    fileChannel.close();
  }
}

class Runner extends RunnableWithCounterAndDone {
  FileAppender<Object> fa;

  Runner(FileAppender<Object> fa) {
    this.fa = fa;
  }

  public void run() {
    while (!isDone()) {
      counter++;
      fa.doAppend("hello " + counter);
      if (counter % 128 == 0) {
        try { Thread.sleep(10);
        } catch (InterruptedException e) { }
      }
    }
  }

}