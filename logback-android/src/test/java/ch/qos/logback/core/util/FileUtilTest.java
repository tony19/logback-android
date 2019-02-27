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
package ch.qos.logback.core.util;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileUtilTest {

  Context context = new ContextBase();
  FileUtil fileUtil = new FileUtil(context);
  List<File> cleanupList = new ArrayList<File>();
  // test-output folder is not always clean
  int diff =  new Random().nextInt(10000);

  @Before
  public void setUp() throws Exception {
    
  }

  @After
  public void tearDown() throws Exception {
    for(File f: cleanupList) {
      f.delete();
    }
  }

  @Test
  public void checkParentCreationInquiryAndSubsequentCreation() {
    File file = new File(CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff+"/testing.txt");
    // these will be deleted later
    cleanupList.add(file);
    cleanupList.add(file.getParentFile());

    assertFalse(file.getParentFile().exists());
    assertTrue(FileUtil.createMissingParentDirectories(file));
    assertTrue(file.getParentFile().exists());
  }
  
  @Test
  public void checkDeeperParentCreationInquiryAndSubsequentCreation() {

    File file = new File(CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff+"/bla/testing.txt");
    // these will be deleted later
    cleanupList.add(file);
    cleanupList.add(file.getParentFile());
    cleanupList.add(file.getParentFile().getParentFile());

    assertFalse(file.getParentFile().exists());
    assertTrue(FileUtil.createMissingParentDirectories(file));
    assertTrue(file.getParentFile().exists());
  }

  @Test
  public void basicCopyingWorks() throws IOException {
    String dir = CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff;

    File dirFile  = new File(dir);
    dirFile.mkdir();

    String src = CoreTestConstants.TEST_INPUT_PREFIX + "compress1.copy";
    String target = CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff+"/copyingWorks.txt";

    fileUtil.copy(src, target);
    Compare.compare(src, target);
  }

  @Test
  public void createParentDirIgnoresExistingDir() {
    String target = CoreTestConstants.OUTPUT_DIR_PREFIX + "/fu" + diff + "/testing.txt";
    File file = new File(target);
    cleanupList.add(file);
    file.mkdirs();
    assertTrue(file.getParentFile().exists());
    assertTrue(FileUtil.createMissingParentDirectories(file));
  }

  @Test
  public void createParentDirAcceptsNoParentSpecified() {
    File file = new File("testing.txt");
    assertTrue(FileUtil.createMissingParentDirectories(file));
  }
}
