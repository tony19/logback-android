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
package ch.qos.logback.core.issue.lbcore258;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;


/**
 *  FileLockSimulator is a small application intended to simulate FileAppender in prudent mode.
 * In this mode, the application obtains an exclusive lock on the file, writes to the file and
 * then releases the lock.
 *
 * <pre>  Usage:
 *   java  FileLockSimulator instanceName pathToLogFile delay
 * where
 * "instanceName" is the name given to the current instance of the application
 * "pathToLogFile" is the path to the log file
 * "delay" is the number of milliseconds of sleep observed every 128 writes
 * </pre>
 *
 * <b>This small application requires only the JDK to compile and to execute.</b>
 *
 * <p>FileLockSimulator should be launched as many times and from as many hosts as there will be
 * JVMs writing to a log file in prudent mode. Performance should be quite good if
 * "pathToLogFile" is on a local file system. On networked file systems such as NFS, performance
 * depends on the speed of the network and NFS implementation. It has been observed that file
 * locking over NFS is biased so that the current owner of the lock is favored over other processes.
 * Thus, while one process hogs the lock for the log file, other processes starve waiting for the
 * lock to the point of appearing deadlocked.
 *
 */
public class FileLockSimulator  {

  static String LINE_SEPARATOR = System.getProperty("line.separator");
  static final int DOT_FREQ = 128;
  static final int DOT_WITH_NEW_LINE_FREQ = DOT_FREQ * 80;

  static String instanceName;
  static int delay;
  static FileOutputStream fos;
  static FileChannel fileChannel;

  public static void main(String[] args) throws IOException, InterruptedException {

    String instanceName = args[0];
    System.out.println("Instance named as [" + instanceName + "]");

    String fileStr = args[1];
    System.out.println("Output target specified as [" + fileStr + "]");

    int delay = Integer.parseInt(args[2]);
    System.out.println("Sleep delay specified as [" + delay + "] milliseconds");

    fos = new FileOutputStream(fileStr, true);
    fileChannel = fos.getChannel();

    for (int i = 1; ; i++) {
      printDotAndSleep(i);
      lockAndWrite(i);
    }
  }

  static void lockAndWrite(int i) throws InterruptedException, IOException {
    FileLock fileLock = null;
    try {
      fileLock = fileChannel.lock();
      long position = fileChannel.position();
      long size = fileChannel.size();
      if (size != position) {
        fileChannel.position(size);
      }
      String msg = "hello from" + instanceName + " " + i + LINE_SEPARATOR;
      fos.write(msg.getBytes());
    } finally {
      if (fileLock != null) {
        fileLock.release();
      }
    }
  }

  static void printDotAndSleep(int i) throws InterruptedException {
    if (i % DOT_FREQ == 0) {
      System.out.print(".");
      Thread.sleep(delay);
    }
    if (i % DOT_WITH_NEW_LINE_FREQ == 0) System.out.println("");
  }
}