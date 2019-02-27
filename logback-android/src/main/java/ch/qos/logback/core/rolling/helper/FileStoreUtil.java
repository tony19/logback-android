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

import ch.qos.logback.core.rolling.RolloverFailure;

import java.io.File;
import java.lang.reflect.Method;

/**
 * A utility class using functionality available since JDK 1.7.
 *
 * @author ceki
 * @since 1.0.10
 */
public class FileStoreUtil {

  static final String PATH_CLASS_STR = "java.nio.file.Path";
  static final String FILES_CLASS_STR = "java.nio.file.Files";

  /**
   * This method assumes that both files a and b exists.
   *
   * @param a first file
   * @param b second file
   * @return whether files are on same store
   * @throws IllegalArgumentException
   */
  static public boolean areOnSameFileStore(File a, File b) throws RolloverFailure {
    if (!a.exists()) {
      throw new IllegalArgumentException("File [" + a + "] does not exist.");
    }
    if (!b.exists()) {
      throw new IllegalArgumentException("File [" + b + "] does not exist.");
    }

// Implements the following by reflection
//    Path pathA = a.toPath();
//    Path pathB = b.toPath();
//
//    FileStore fileStoreA = Files.getFileStore(pathA);
//    FileStore fileStoreB = Files.getFileStore(pathB);
//
//    return fileStoreA.equals(fileStoreB);

    try {
      Class<?> pathClass = Class.forName(PATH_CLASS_STR);
      Class<?> filesClass = Class.forName(FILES_CLASS_STR);

      Method toPath = File.class.getMethod("toPath");
      Method getFileStoreMethod = filesClass.getMethod("getFileStore", pathClass);


      Object pathA = toPath.invoke(a);
      Object pathB = toPath.invoke(b);

      Object fileStoreA = getFileStoreMethod.invoke(null, pathA);
      Object fileStoreB = getFileStoreMethod.invoke(null, pathB);
      return fileStoreA.equals(fileStoreB);
    } catch (Exception e) {
      throw new RolloverFailure("Failed to check file store equality for [" + a + "] and [" + b + "]", e);
    }
  }
}
