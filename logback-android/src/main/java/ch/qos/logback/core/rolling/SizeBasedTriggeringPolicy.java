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

import java.io.File;

import ch.qos.logback.core.util.FileSize;

/**
 * SizeBasedTriggeringPolicy looks at size of the file being currently written
 * to. If it grows bigger than the specified size, the FileAppender using the
 * SizeBasedTriggeringPolicy rolls the file and creates a new one.
 * 
 * For more information about this policy, please refer to the online manual at
 * http://logback.qos.ch/manual/appenders.html#SizeBasedTriggeringPolicy
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 */
public class SizeBasedTriggeringPolicy<E> extends TriggeringPolicyBase<E> {

  public static final String SEE_SIZE_FORMAT = "http://logback.qos.ch/codes.html#sbtp_size_format";
  /**
   * The default maximum file size.
   */
  public static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

  FileSize maxFileSize = new FileSize(DEFAULT_MAX_FILE_SIZE);

  public SizeBasedTriggeringPolicy() {
  }

  public boolean isTriggeringEvent(final File activeFile, final E event) {
    return (activeFile.length() >= maxFileSize.getSize());
  }

  public FileSize getMaxFileSize() {
    return this.maxFileSize;
  }

  public void setMaxFileSize(FileSize maxFileSize) {
    this.maxFileSize = maxFileSize;
  }
}
