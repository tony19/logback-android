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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.util.FileSize;

public class SizeBasedTriggeringPolicyTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  SizeBasedTriggeringPolicy<Object> policy = new SizeBasedTriggeringPolicy<Object>();

  private File fileOfLength(int length) throws IOException {
    File file = tmp.newFile();
    Files.write(file.toPath(), new byte[length]);
    return file;
  }

  @Test
  public void defaultMaxFileSizeIsTenMegabytes() {
    assertEquals(10 * 1024 * 1024, policy.getMaxFileSize().getSize());
    assertEquals(SizeBasedTriggeringPolicy.DEFAULT_MAX_FILE_SIZE, policy.getMaxFileSize().getSize());
  }

  @Test
  public void maxFileSizeIsTheOneSet() {
    FileSize maxFileSize = new FileSize(100);

    policy.setMaxFileSize(maxFileSize);

    assertSame(maxFileSize, policy.getMaxFileSize());
  }

  @Test
  public void triggersOnceActiveFileReachesMaxFileSize() throws IOException {
    policy.setMaxFileSize(new FileSize(100));

    assertFalse(policy.isTriggeringEvent(fileOfLength(99), null));
    assertTrue(policy.isTriggeringEvent(fileOfLength(100), null));
  }
}
