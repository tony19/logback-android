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

import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.FileSize;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class SizeBasedRollingTest extends ScaffoldingForRollingTests {

  RollingFileAppender<Object> rfa = new RollingFileAppender<Object>();
  FixedWindowRollingPolicy fwrp = new FixedWindowRollingPolicy();
  SizeBasedTriggeringPolicy<Object> sizeBasedTriggeringPolicy = new SizeBasedTriggeringPolicy<Object>();
  EchoEncoder<Object> encoder = new EchoEncoder<Object>();


  @Before
  @Override
  public void setUp() throws ParseException {
    super.setUp();
    fwrp.setContext(context);
    fwrp.setParent(rfa);
    rfa.setContext(context);
    sizeBasedTriggeringPolicy.setContext(context);
  }

  private void initRFA(String filename) {
    rfa.setEncoder(encoder);
    if (filename != null) {
      rfa.setFile(filename);
    }
  }

  /**
   * Test whether FixedWindowRollingPolicy throws an exception when the
   * ActiveFileName is not set.
   */
  @Test(expected = IllegalStateException.class)
  public void activeFileNameNotSet() {
    sizeBasedTriggeringPolicy.setMaxFileSize(new FileSize(100));
    sizeBasedTriggeringPolicy.start();

    fwrp.setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "sizeBased-test1.%i");
    fwrp.start();
    // The absence of activeFileName option should cause an exception.
  }


  void generic(String testName, String fileName, String filenamePattern, List<String> expectedFilenameList) throws InterruptedException, IOException {
    rfa.setName("ROLLING");
    initRFA(randomOutputDir + fileName);

    sizeBasedTriggeringPolicy.setMaxFileSize(new FileSize(100));
    fwrp.setMinIndex(0);
    fwrp.setFileNamePattern(randomOutputDir + filenamePattern);

    rfa.triggeringPolicy = sizeBasedTriggeringPolicy;
    rfa.rollingPolicy = fwrp;

    fwrp.start();
    sizeBasedTriggeringPolicy.start();
    rfa.start();

    int runLength = 40;
    String prefix = "hello";
    for (int i = 0; i < runLength; i++){
      Thread.sleep(10);
      rfa.doAppend(prefix + i);
    }
    rfa.stop();

    existenceCheck(expectedFilenameList);
    reverseSortedContentCheck(randomOutputDir, runLength, prefix);
  }

  @Test
  public void smoke() throws IOException, InterruptedException {
    expectedFilenameList.add(randomOutputDir + "a-sizeBased-smoke.log");
    expectedFilenameList.add(randomOutputDir + "sizeBased-smoke.0");
    expectedFilenameList.add(randomOutputDir + "sizeBased-smoke.1");
    generic("zipped", "a-sizeBased-smoke.log", "sizeBased-smoke.%i", expectedFilenameList);

  }
  @Test
  public void gz() throws IOException, InterruptedException {
    expectedFilenameList.add(randomOutputDir + "a-sbr-gzed.log");
    expectedFilenameList.add(randomOutputDir + "sbr-gzed.0.gz");
    expectedFilenameList.add(randomOutputDir + "sbr-gzed.1.gz");
    generic("gzed", "a-sbr-gzed.log", "sbr-gzed.%i.gz", expectedFilenameList);
  }

  // see also LBCORE-199
  @Test
  public void zipped() throws IOException, InterruptedException  {
    expectedFilenameList.add(randomOutputDir + "a-sbr-zipped.log");
    expectedFilenameList.add(randomOutputDir + "sbr-zipped.0.zip");
    expectedFilenameList.add(randomOutputDir + "sbr-zipped.1.zip");
    generic("zipped", "a-sbr-zipped.log", "sbr-zipped.%i.zip", expectedFilenameList);

    List<String> zipFiles = filterElementsInListBySuffix(".zip");
    zipEntryNameCheck(zipFiles, "sbr-zipped.20\\d{2}-\\d{2}-\\d{2}_\\d{4}");
  }
}
