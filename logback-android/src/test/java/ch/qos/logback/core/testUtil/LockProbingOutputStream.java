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
package ch.qos.logback.core.testUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;

/**
 * A {@link ResilientFileOutputStream} (in append mode) that records, for each
 * write, whether this JVM holds a lock on its file at that moment, i.e.
 * whether a prudent appender writes under the file lock.
 */
public class LockProbingOutputStream extends ResilientFileOutputStream {

  private final List<Boolean> lockHeldDuringWrite = new ArrayList<Boolean>();

  public LockProbingOutputStream(File file) throws FileNotFoundException {
    super(file, true, FileAppender.DEFAULT_BUFFER_SIZE);
  }

  /**
   * @return for each write of at least one byte so far, whether this JVM
   * held a lock on the file
   */
  public synchronized List<Boolean> getLockHeldDuringWrite() {
    return new ArrayList<Boolean>(lockHeldDuringWrite);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    // an empty write writes nothing, and may come while the channel is closed
    if (len > 0) {
      synchronized (this) {
        lockHeldDuringWrite.add(isLockedByThisJvm());
      }
    }
    super.write(b, off, len);
  }

  // File locks are held on behalf of the entire JVM: locking the file throws
  // OverlappingFileLockException while any channel of this JVM holds a lock on
  // it. This stream's own channel is used, because closing another channel
  // on the file may release the locks that this JVM holds on it.
  private boolean isLockedByThisJvm() {
    try {
      FileLock lock = getChannel().tryLock();
      if (lock != null) {
        lock.release();
      }
      return false;
    } catch (OverlappingFileLockException e) {
      return true;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
