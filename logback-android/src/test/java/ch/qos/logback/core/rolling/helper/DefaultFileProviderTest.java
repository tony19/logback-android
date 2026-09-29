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
package ch.qos.logback.core.rolling.helper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultFileProviderTest {

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private final DefaultFileProvider fileProvider = new DefaultFileProvider();

  @Test
  public void existsReflectsTheFileSystem() throws IOException {
    File file = tmpDir.newFile("app.log");
    File dir = tmpDir.newFolder("logs");
    File missing = new File(tmpDir.getRoot(), "missing.log");

    assertTrue(fileProvider.exists(file));
    assertTrue(fileProvider.exists(dir));
    assertFalse(fileProvider.exists(missing));

    assertTrue(fileProvider.deleteFile(file));
    assertFalse(fileProvider.exists(file));
  }
}
